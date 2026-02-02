package com.shotaroi.keygateapi.usage;

import com.shotaroi.keygateapi.ratelimit.RedisRateLimiter;
import com.shotaroi.keygateapi.security.ApiPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsageController {

    private final RedisRateLimiter rateLimiter;

    public UsageController(RedisRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public record UsageResponse(
            String clientName,
            int limitPerMinute,
            long usedThisMinute,
            long remainingThisMinute,
            long resetsInSeconds
    ) {}

    @GetMapping("/usage")
    public UsageResponse usage(Authentication auth) {
        ApiPrincipal principal = (ApiPrincipal) auth.getPrincipal();

        long used = rateLimiter.currentUsed(principal.apiKeyHash());
        int limit = principal.requestsPerMinute();

        long remaining = Math.max(0, (long) limit - used);
        long reset = rateLimiter.secondsUntilReset();

        return new UsageResponse(
                principal.name(),
                limit,
                used,
                remaining,
                reset
        );
    }
}
