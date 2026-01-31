package com.shotaroi.keygateapi.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class RedisRateLimiter {

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public boolean allowRequest(String apiKeyHash, int limitPerMinute) {
        // Example key: rl:<hash>:<minute>
        long minuteBucket = Instant.now().truncatedTo(ChronoUnit.MINUTES).getEpochSecond();
        String key = "rl:" + apiKeyHash + ":" + minuteBucket;

        Long count = redis.opsForValue().increment(key);

        // Set the key to expire after 70 seconds so Redis cleans it up
        if (count != null && count == 1) {
            redis.expire(key, java.time.Duration.ofSeconds(70));
        }

        return count != null && count <= limitPerMinute;
    }
}
