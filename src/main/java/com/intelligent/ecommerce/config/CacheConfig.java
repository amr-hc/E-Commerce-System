package com.intelligent.ecommerce.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    private RedisCacheConfiguration createCacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();
    }

    @Bean("caffeineCacheManager")
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000));
        return cacheManager;
    }

    @Bean("shortTtlCacheManager")
    public RedisCacheManager shortTtlCacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
                .cacheDefaults(createCacheConfig(Duration.ofMinutes(5)))
                .build();
    }

    @Bean("longTtlCacheManager")
    public RedisCacheManager longTtlCacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
                .cacheDefaults(createCacheConfig(Duration.ofHours(6)))
                .build();
    }

    // Default cache manager that Spring will use if no cacheManager is specified in @Cacheable
    @Bean
    @Primary
    public RedisCacheManager defaultCacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.builder(factory)
                .cacheDefaults(createCacheConfig(Duration.ofMinutes(10)))
                .build();
    }
}
