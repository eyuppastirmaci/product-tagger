package com.producttagger.backend.shared.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables the Spring cache abstraction. The provider (Caffeine today) and its
 * sizing/TTL live entirely in application.yaml, so swapping to Redis later is
 * a dependency and configuration change without touching any @Cacheable code.
 */
@Configuration
@EnableCaching
class CacheConfig {
}
