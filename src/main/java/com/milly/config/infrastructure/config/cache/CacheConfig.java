package com.milly.config.infrastructure.config.cache;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    @Bean
    CaffeineCacheFactory caffeineCacheFactory(CacheProperties properties) {
        return new CaffeineCacheFactory(properties);
    }

    @Bean
    CacheManager cacheManager(List<CaffeineCache> caches) {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(caches);
        cacheManager.initializeCaches();
        return cacheManager;
    }
}
