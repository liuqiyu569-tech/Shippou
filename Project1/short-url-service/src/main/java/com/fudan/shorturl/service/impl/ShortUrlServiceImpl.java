package com.fudan.shorturl.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fudan.shorturl.common.exception.BusinessException;
import com.fudan.shorturl.config.RabbitConfig;
import com.fudan.shorturl.dto.AccessEvent;
import com.fudan.shorturl.dto.CreateShortUrlRequest;
import com.fudan.shorturl.dto.ShortUrlVO;
import com.fudan.shorturl.entity.LongUrlIndex;
import com.fudan.shorturl.entity.ShortUrl;
import com.fudan.shorturl.mapper.LongUrlIndexMapper;
import com.fudan.shorturl.mapper.ShortUrlMapper;
import com.fudan.shorturl.service.ShortUrlService;
import com.fudan.shorturl.util.Base62;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortUrlServiceImpl implements ShortUrlService {

    private static final String CACHE_KEY_PREFIX = "short:url:";
    private static final String LOCK_KEY_PREFIX = "lock:rebuild:";
    private static final String NULL_SENTINEL = "__NULL__";
    private static final Duration DEFAULT_TTL = Duration.ofHours(12);
    private static final Duration NULL_TTL = Duration.ofMinutes(5);
    private static final int TTL_JITTER_SECONDS = 60;
    private static final long LOCK_WAIT_MILLIS = 200;
    private static final long LOCK_LEASE_SECONDS = 5;

    private final ShortUrlMapper shortUrlMapper;
    private final LongUrlIndexMapper longUrlIndexMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RBloomFilter<String> shortCodeBloomFilter;
    private final RedissonClient redissonClient;
    private final Cache<String, String> shortUrlLocalCache;
    private final RabbitTemplate rabbitTemplate;

    @Value("${shorturl.domain}")
    private String domain;

    @Override
    public ShortUrlVO createShortUrl(CreateShortUrlRequest req) {
        long hash = fnv1aHash(req.getLongUrl());

        // Week 7: 先查全局索引（t_long_url_index 单表 PK 查询），命中后只对一个分片做 SK 查询
        // 老逻辑 WHERE long_url_hash=? AND long_url=? 在分片表上会变成 4 表广播查询，破坏分片意义
        LongUrlIndex idx = longUrlIndexMapper.selectById(hash);
        if (idx != null) {
            ShortUrl existing = shortUrlMapper.selectOne(
                    Wrappers.<ShortUrl>lambdaQuery()
                            .eq(ShortUrl::getShortCode, idx.getShortCode())
                            .last("LIMIT 1")
            );
            // 二次校验 long_url：防 FNV-1a 64bit 极小概率的 hash 碰撞（不同 URL 同 hash）
            if (existing != null && req.getLongUrl().equals(existing.getLongUrl())) {
                log.debug("命中已有短链(全局索引): {}", existing.getShortCode());
                writeCache(existing);
                return toVO(existing);
            }
        }

        long id = IdUtil.getSnowflakeNextId();
        String shortCode = Base62.encode(id);

        ShortUrl record = new ShortUrl();
        record.setId(id);
        record.setShortCode(shortCode);
        record.setLongUrl(req.getLongUrl());
        record.setLongUrlHash(hash);
        record.setExpireTime(req.getExpireTime());
        record.setAccessCount(0L);

        shortUrlMapper.insert(record);

        // 同步写全局索引；DuplicateKeyException 表示并发竞争中我们慢了一步——
        // 回查索引拿到对手已插入的 shortCode 返回，避免给用户两条短链
        try {
            LongUrlIndex newIdx = new LongUrlIndex();
            newIdx.setLongUrlHash(hash);
            newIdx.setShortCode(shortCode);
            longUrlIndexMapper.insert(newIdx);
        } catch (DuplicateKeyException race) {
            LongUrlIndex winner = longUrlIndexMapper.selectById(hash);
            if (winner != null && !winner.getShortCode().equals(shortCode)) {
                log.info("并发创建竞争失败 hash={} ours={} winner={}", hash, shortCode, winner.getShortCode());
                ShortUrl winnerRecord = shortUrlMapper.selectOne(
                        Wrappers.<ShortUrl>lambdaQuery()
                                .eq(ShortUrl::getShortCode, winner.getShortCode())
                                .last("LIMIT 1")
                );
                if (winnerRecord != null) {
                    writeCache(winnerRecord);
                    return toVO(winnerRecord);
                }
            }
        }

        shortCodeBloomFilter.add(shortCode);
        writeCache(record);
        return toVO(record);
    }

    @Override
    public String getLongUrl(String shortCode) {
        // L1 优先：Redis 故障时 L1 是唯一兜底（防雪崩）
        String local = shortUrlLocalCache.getIfPresent(shortCode);
        if (local != null) {
            log.debug("L1 (Caffeine) 命中: {}", shortCode);
            return handleCached(shortCode, local);
        }

        // 布隆过滤器拦截非法 shortCode（防穿透）；Redis 故障时跳过
        try {
            if (!shortCodeBloomFilter.contains(shortCode)) {
                log.debug("布隆过滤器拦截: {}", shortCode);
                throw new BusinessException(404, "短链不存在");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("布隆过滤器查询失败，降级跳过: {}", e.getMessage());
        }

        String cacheKey = CACHE_KEY_PREFIX + shortCode;

        // L2 Redis；故障时降级查 DB
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return handleCached(shortCode, cached);
            }
        } catch (Exception e) {
            log.warn("L2 Redis 查询失败，降级查 DB: {}", e.getMessage());
            return loadFromDbAndCache(shortCode, cacheKey);
        }

        return rebuildWithLock(shortCode, cacheKey);
    }

    /** 处理已读到的缓存值：写 L1 → 识别空值哨兵 → 异步计数 → 返回 longUrl */
    private String handleCached(String shortCode, String cached) {
        shortUrlLocalCache.put(shortCode, cached);
        if (NULL_SENTINEL.equals(cached)) {
            log.debug("命中空值哨兵: {}", shortCode);
            throw new BusinessException(404, "短链不存在");
        }
        log.debug("缓存命中: {}", shortCode);
        incrementAccessCountAsync(shortCode);
        return cached;
    }

    /** 缓存 miss：用 Redisson RLock 互斥重建，避免热点 key 击穿 */
    private String rebuildWithLock(String shortCode, String cacheKey) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + shortCode);
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_WAIT_MILLIS, LOCK_LEASE_SECONDS * 1000L, TimeUnit.MILLISECONDS);
            if (locked) {
                // double-check：拿到锁后再查一次缓存，别人可能已经回写
                String cached = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return handleCached(shortCode, cached);
                }
                return loadFromDbAndCache(shortCode, cacheKey);
            }
            // 没抢到锁：短暂等待后回查缓存（大概率别人已经写好）
            log.debug("未抢到重建锁，回查缓存: {}", shortCode);
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return handleCached(shortCode, cached);
            }
            // 仍 miss → 退化为直接查 DB（避免无限等待）
            return loadFromDbAndCache(shortCode, cacheKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "缓存重建被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /** 查 DB 并写缓存（空值哨兵 / 正常 longUrl），同时填充 L1 */
    private String loadFromDbAndCache(String shortCode, String cacheKey) {
        ShortUrl record = shortUrlMapper.selectOne(
                Wrappers.<ShortUrl>lambdaQuery()
                        .eq(ShortUrl::getShortCode, shortCode)
                        .last("LIMIT 1")
        );
        if (record == null) {
            stringRedisTemplate.opsForValue().set(cacheKey, NULL_SENTINEL, NULL_TTL);
            shortUrlLocalCache.put(shortCode, NULL_SENTINEL);
            log.debug("写入空值哨兵: {}", shortCode);
            throw new BusinessException(404, "短链不存在");
        }
        if (record.getExpireTime() != null
                && record.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(410, "短链已过期");
        }
        writeCache(record);
        shortUrlLocalCache.put(shortCode, record.getLongUrl());
        incrementAccessCountAsync(shortCode);
        return record.getLongUrl();
    }

    private void writeCache(ShortUrl record) {
        Duration ttl = computeTtl(record.getExpireTime());
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                CACHE_KEY_PREFIX + record.getShortCode(),
                record.getLongUrl(),
                ttl
        );
    }

    /** TTL = min(过期剩余, 默认 12h) + 随机抖动，防止集中过期导致雪崩 */
    private Duration computeTtl(LocalDateTime expireTime) {
        Duration base = DEFAULT_TTL;
        if (expireTime != null) {
            Duration remaining = Duration.between(LocalDateTime.now(), expireTime);
            if (remaining.isNegative() || remaining.isZero()) {
                return Duration.ZERO;
            }
            base = remaining.compareTo(DEFAULT_TTL) < 0 ? remaining : DEFAULT_TTL;
        }
        int jitter = ThreadLocalRandom.current().nextInt(TTL_JITTER_SECONDS);
        return base.plusSeconds(jitter);
    }

    /** Week 5: 投递访问事件到 RabbitMQ，由 AccessCountConsumer 异步批量入库 */
    private void incrementAccessCountAsync(String shortCode) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE,
                    RabbitConfig.ACCESS_ROUTING_KEY,
                    new AccessEvent(shortCode, System.currentTimeMillis())
            );
        } catch (Exception e) {
            // MQ 故障不应影响主流程（短链跳转），仅记日志降级
            log.warn("访问计数事件投递失败 shortCode={}: {}", shortCode, e.getMessage());
        }
    }

    private ShortUrlVO toVO(ShortUrl r) {
        ShortUrlVO vo = new ShortUrlVO();
        vo.setShortCode(r.getShortCode());
        vo.setShortUrl(domain + "/" + r.getShortCode());
        vo.setLongUrl(r.getLongUrl());
        return vo;
    }

    /** FNV-1a 64-bit：分布均匀、计算极快，用作长链去重索引 */
    private long fnv1aHash(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }
}
