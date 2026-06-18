package com.fudan.shorturl;

import com.fudan.shorturl.mapper.LongUrlIndexMapper;
import com.fudan.shorturl.mapper.ShortUrlMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class ShortUrlServiceApplicationTests {

	@MockitoBean
	private ShortUrlMapper shortUrlMapper;

	@MockitoBean
	private LongUrlIndexMapper longUrlIndexMapper;

	@MockitoBean
	private StringRedisTemplate stringRedisTemplate;

	@MockitoBean
	private RBloomFilter<String> shortCodeBloomFilter;

	@MockitoBean
	private RedissonClient redissonClient;

	@MockitoBean
	private RabbitTemplate rabbitTemplate;

	@BeforeEach
	void setUp() {
		when(shortCodeBloomFilter.contains(anyString())).thenReturn(false);
	}

	@Test
	void contextLoads() {
	}

}
