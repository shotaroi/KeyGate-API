package com.shotaroi.keygateapi.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class RedisRateLimiter {

    private final StringRedisTemplate redis;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public long currentUsed(String apiKeyHash) {
        long minuteBucket = currentMinuteBucket();
        String key = keyFor(apiKeyHash, minuteBucket);

        String value = redis.opsForValue().get(key);
        if (value == null) return 0;

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public long secondsUntilReset() {
        long now = Instant.now().getEpochSecond();
        long passed = now % 60;
        long left = 60 - passed;
        return (left == 0) ? 60 : left;
    }

    private long currentMinuteBucket() {
        return Instant.now().truncatedTo(ChronoUnit.MINUTES).getEpochSecond();
    }

    private String keyFor(String apiKeyHash, long minuteBucket) {
        return "rl:" + apiKeyHash + ":" + minuteBucket;
    }


    public boolean allowRequest(String apiKeyHash, int limitPerMinute) {
        long minuteBucket = currentMinuteBucket();
        String key = keyFor(apiKeyHash, minuteBucket);

        Long count = redis.opsForValue().increment(key);

        if (count != null && count == 1) {
            redis.expire(key, Duration.ofSeconds(70));
        }

        return count != null && count <= limitPerMinute;
    }

}
