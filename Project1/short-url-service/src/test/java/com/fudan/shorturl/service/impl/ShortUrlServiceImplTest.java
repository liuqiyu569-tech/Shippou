package com.fudan.shorturl.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fudan.shorturl.common.exception.BusinessException;
import com.fudan.shorturl.dto.CreateShortUrlRequest;
import com.fudan.shorturl.dto.ShortUrlVO;
import com.fudan.shorturl.entity.LongUrlIndex;
import com.fudan.shorturl.entity.ShortUrl;
import com.fudan.shorturl.mapper.LongUrlIndexMapper;
import com.fudan.shorturl.mapper.ShortUrlMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceImplTest {

    private static final String DOMAIN = "http://short.test";

    @Mock
    private ShortUrlMapper shortUrlMapper;

    @Mock
    private LongUrlIndexMapper longUrlIndexMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RBloomFilter<String> shortCodeBloomFilter;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private Cache<String, String> localCache;
    private ShortUrlServiceImpl service;

    @BeforeEach
    void setUp() {
        localCache = Caffeine.newBuilder().maximumSize(100).build();
        service = new ShortUrlServiceImpl(
                shortUrlMapper,
                longUrlIndexMapper,
                stringRedisTemplate,
                shortCodeBloomFilter,
                redissonClient,
                localCache,
                rabbitTemplate
        );
        ReflectionTestUtils.setField(service, "domain", DOMAIN);
    }

    @Test
    void createShortUrlCreatesRecordAndWarmsCache() {
        stubRedisValueOperations();
        CreateShortUrlRequest request = request("https://www.fudan.edu.cn");
        when(longUrlIndexMapper.selectById(anyLong())).thenReturn(null);

        ShortUrlVO vo = service.createShortUrl(request);

        assertThat(vo.getLongUrl()).isEqualTo(request.getLongUrl());
        assertThat(vo.getShortCode()).isNotBlank();
        assertThat(vo.getShortUrl()).isEqualTo(DOMAIN + "/" + vo.getShortCode());

        ArgumentCaptor<ShortUrl> recordCaptor = ArgumentCaptor.forClass(ShortUrl.class);
        verify(shortUrlMapper).insert(recordCaptor.capture());
        ShortUrl inserted = recordCaptor.getValue();
        assertThat(inserted.getLongUrl()).isEqualTo(request.getLongUrl());
        assertThat(inserted.getShortCode()).isEqualTo(vo.getShortCode());
        assertThat(inserted.getAccessCount()).isZero();

        verify(longUrlIndexMapper).insert(any(LongUrlIndex.class));
        verify(shortCodeBloomFilter).add(vo.getShortCode());
        verify(valueOperations).set(eq("short:url:" + vo.getShortCode()), eq(request.getLongUrl()), any(Duration.class));
    }

    @Test
    void createShortUrlReturnsExistingWhenLongUrlIndexHits() {
        stubRedisValueOperations();
        CreateShortUrlRequest request = request("https://www.fudan.edu.cn");
        LongUrlIndex index = new LongUrlIndex();
        index.setLongUrlHash(1L);
        index.setShortCode("abc123");
        ShortUrl existing = shortUrl("abc123", request.getLongUrl(), null);

        when(longUrlIndexMapper.selectById(anyLong())).thenReturn(index);
        when(shortUrlMapper.selectOne(anyWrapper())).thenReturn(existing);

        ShortUrlVO vo = service.createShortUrl(request);

        assertThat(vo.getShortCode()).isEqualTo("abc123");
        assertThat(vo.getLongUrl()).isEqualTo(request.getLongUrl());
        assertThat(vo.getShortUrl()).isEqualTo(DOMAIN + "/abc123");
        verify(shortUrlMapper, never()).insert(any(ShortUrl.class));
        verify(longUrlIndexMapper, never()).insert(any(LongUrlIndex.class));
        verify(valueOperations).set(eq("short:url:abc123"), eq(request.getLongUrl()), any(Duration.class));
    }

    @Test
    void getLongUrlReturns404WhenBloomFilterRejectsShortCode() {
        when(shortCodeBloomFilter.contains("missing")).thenReturn(false);

        assertThatThrownBy(() -> service.getLongUrl("missing"))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(404);
                    assertThat(e.getMessage()).isEqualTo("短链不存在");
                });

        verify(shortUrlMapper, never()).selectOne(anyWrapper());
        verify(valueOperations, never()).get(anyString());
    }

    @Test
    void getLongUrlReturns410WhenDbRecordExpired() throws Exception {
        stubRedisValueOperations();
        when(shortCodeBloomFilter.contains("expired")).thenReturn(true);
        when(valueOperations.get("short:url:expired")).thenReturn(null);
        when(redissonClient.getLock("lock:rebuild:expired")).thenReturn(lock);
        when(lock.tryLock(200, 5_000, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(shortUrlMapper.selectOne(anyWrapper()))
                .thenReturn(shortUrl("expired", "https://expired.example", LocalDateTime.now().minusMinutes(1)));

        assertThatThrownBy(() -> service.getLongUrl("expired"))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(410);
                    assertThat(e.getMessage()).isEqualTo("短链已过期");
                });

        verify(lock).unlock();
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void getLongUrlUsesRedisCacheWithoutQueryingDb() {
        stubRedisValueOperations();
        when(shortCodeBloomFilter.contains("abc123")).thenReturn(true);
        when(valueOperations.get("short:url:abc123")).thenReturn("https://cached.example");

        String longUrl = service.getLongUrl("abc123");

        assertThat(longUrl).isEqualTo("https://cached.example");
        assertThat(localCache.getIfPresent("abc123")).isEqualTo("https://cached.example");
        verify(shortUrlMapper, never()).selectOne(anyWrapper());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void getLongUrlIgnoresRabbitFailureOnCacheHit() {
        stubRedisValueOperations();
        when(shortCodeBloomFilter.contains("abc123")).thenReturn(true);
        when(valueOperations.get("short:url:abc123")).thenReturn("https://cached.example");
        doThrow(new RuntimeException("mq down"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        String longUrl = service.getLongUrl("abc123");

        assertThat(longUrl).isEqualTo("https://cached.example");
        verify(shortUrlMapper, never()).selectOne(anyWrapper());
    }

    @SuppressWarnings("unchecked")
    private Wrapper<ShortUrl> anyWrapper() {
        return any(Wrapper.class);
    }

    private void stubRedisValueOperations() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private CreateShortUrlRequest request(String longUrl) {
        CreateShortUrlRequest request = new CreateShortUrlRequest();
        request.setLongUrl(longUrl);
        return request;
    }

    private ShortUrl shortUrl(String shortCode, String longUrl, LocalDateTime expireTime) {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setId(1L);
        shortUrl.setShortCode(shortCode);
        shortUrl.setLongUrl(longUrl);
        shortUrl.setLongUrlHash(1L);
        shortUrl.setExpireTime(expireTime);
        shortUrl.setAccessCount(0L);
        return shortUrl;
    }
}
