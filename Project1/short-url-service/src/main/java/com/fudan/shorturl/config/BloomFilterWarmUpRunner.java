package com.fudan.shorturl.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fudan.shorturl.entity.ShortUrl;
import com.fudan.shorturl.mapper.ShortUrlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class BloomFilterWarmUpRunner implements ApplicationRunner {

    private final RBloomFilter<String> shortCodeBloomFilter;
    private final ShortUrlMapper shortUrlMapper;

    @Override
    public void run(ApplicationArguments args) {
        long count = shortCodeBloomFilter.count();
        if (count > 0) {
            log.info("布隆过滤器已有 {} 个元素，跳过预热", count);
            return;
        }
        List<ShortUrl> all = shortUrlMapper.selectList(
                Wrappers.<ShortUrl>lambdaQuery().select(ShortUrl::getShortCode)
        );
        for (ShortUrl s : all) {
            shortCodeBloomFilter.add(s.getShortCode());
        }
        log.info("布隆过滤器预热完成，灌入 {} 个 shortCode（容量 {} / 误判率 {}）",
                all.size(),
                BloomFilterConfig.EXPECTED_INSERTIONS,
                BloomFilterConfig.FALSE_PROBABILITY);
    }
}
