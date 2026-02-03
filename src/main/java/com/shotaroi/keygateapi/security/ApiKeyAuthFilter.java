package com.shotaroi.keygateapi.security;

import com.shotaroi.keygateapi.api.ApiClient;
import com.shotaroi.keygateapi.api.ApiClientRepository;
import com.shotaroi.keygateapi.ratelimit.RedisRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiClientRepository repo;
    private final ApiKeyHasher hasher;
    private final RedisRateLimiter rateLimiter;

    public ApiKeyAuthFilter(ApiClientRepository repo, ApiKeyHasher hasher, RedisRateLimiter rateLimiter) {
        this.repo = repo;
        this.hasher = hasher;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

// Public endpoints that should NOT require X-API-KEY
        if (path.startsWith("/public")
                || path.startsWith("/clients")
                || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }


        String rawKey = request.getHeader("X-API-KEY");
        if (rawKey == null || rawKey.isBlank()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String hash = hasher.sha256(rawKey);
        ApiClient client = repo.findByApiKeyHash(hash).orElse(null);

        if (client == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        boolean allowed = rateLimiter.allowRequest(client.getApiKeyHash(), client.getRequestsPerMinute());

        long resetSeconds = rateLimiter.secondsUntilReset();
        int limit = client.getRequestsPerMinute();

        // Since allowRequest() incremented and we exceeded, used will be > limit
        long used = rateLimiter.currentUsed(client.getApiKeyHash());
        long remaining = 0;
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetSeconds));

        if (!allowed) {
            response.resetBuffer();
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());

            response.setHeader("Retry-After", String.valueOf(resetSeconds));

            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"rate_limited\",\"retryAfterSeconds\":" + resetSeconds +
                            ",\"limitPerMinute\":" + limit +
                            ",\"usedThisMinute\":" + used + "}"
            );
            response.flushBuffer();
            return;
        }


        var principal = new ApiPrincipal(
                client.getName(),
                client.getApiKeyHash(),
                client.getRequestsPerMinute()
        );

        var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);


        filterChain.doFilter(request, response);
    }

    private long secondsUntilNextMinute() {
        long now = java.time.Instant.now().getEpochSecond();
        long secondsPassedThisMinute = now % 60;
        long left = 60 - secondsPassedThisMinute;
        return (left == 0) ? 60 : left;
    }

}
