package com.fudan.shorturl.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class BloomFilterConfig {

    public static final String SHORT_CODE_BLOOM = "bloom:short-code";

    public static final long EXPECTED_INSERTIONS = 1_000_000L;
    public static final double FALSE_PROBABILITY = 0.001;

    @Bean
    public RBloomFilter<String> shortCodeBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter(SHORT_CODE_BLOOM);
        bloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);
        return bloomFilter;
    }
}
