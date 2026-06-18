package com.fudan.shorturl.consumer;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fudan.shorturl.config.RabbitConfig;
import com.fudan.shorturl.dto.AccessEvent;
import com.fudan.shorturl.entity.ShortUrl;
import com.fudan.shorturl.mapper.ShortUrlMapper;
import com.rabbitmq.client.Channel;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@EnableScheduling
@Profile("!test")
@RequiredArgsConstructor
public class AccessCountConsumer {

    private static final int FLUSH_THRESHOLD = 1000;

    private final ShortUrlMapper shortUrlMapper;

    /** shortCode → 累计未入库次数 */
    private final ConcurrentHashMap<String, AtomicLong> buffer = new ConcurrentHashMap<>();

    @RabbitListener(queues = RabbitConfig.ACCESS_QUEUE, concurrency = "4-8")
    public void onAccessEvent(AccessEvent event, Message rawMessage, Channel channel) throws Exception {
        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        try {
            buffer.computeIfAbsent(event.getShortCode(), k -> new AtomicLong(0)).incrementAndGet();
            channel.basicAck(deliveryTag, false);

            if (buffer.size() >= FLUSH_THRESHOLD) {
                flush();
            }
        } catch (Exception e) {
            log.error("处理访问事件失败 shortCode={}", event.getShortCode(), e);
            // 失败 nack 不重入队，进入死信队列
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /** 每 5 秒强制 flush 一次，保证延迟上限 */
    @Scheduled(fixedDelay = 5_000)
    public void scheduledFlush() {
        flush();
    }

    @PreDestroy
    public void shutdownFlush() {
        log.info("应用关停前 flush 剩余访问计数");
        flush();
    }

    private synchronized void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        Map<String, Long> snapshot = new HashMap<>();
        buffer.forEach((code, counter) -> {
            long n = counter.getAndSet(0);
            if (n > 0) {
                snapshot.put(code, n);
            }
        });
        // 清理累计为 0 的 key 避免无限增长
        buffer.entrySet().removeIf(e -> e.getValue().get() == 0);

        if (snapshot.isEmpty()) {
            return;
        }

        // 简化版批量：逐条 UPDATE（项目 MVP 先这样，后续可用 CASE WHEN 单 SQL 合并）
        long totalIncrement = 0;
        for (Map.Entry<String, Long> e : snapshot.entrySet()) {
            shortUrlMapper.update(null,
                    Wrappers.<ShortUrl>lambdaUpdate()
                            .setSql("access_count = access_count + " + e.getValue())
                            .eq(ShortUrl::getShortCode, e.getKey())
            );
            totalIncrement += e.getValue();
        }
        log.info("flush 完成: {} 个 shortCode，共 {} 次访问写入 DB", snapshot.size(), totalIncrement);
    }
}
