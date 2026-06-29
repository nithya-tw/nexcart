package com.nexcart.productservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;

/**
 * Test configuration to disable caching for integration tests.
 * Provides a NoOpCacheManager that does nothing, avoiding Redis dependency.
 */
@TestConfiguration
public class TestCacheConfig {
    
    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}
