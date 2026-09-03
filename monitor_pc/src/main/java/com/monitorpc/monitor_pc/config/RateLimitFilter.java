package com.monitorpc.monitor_pc.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> loginBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(2))
            .maximumSize(10_000)
            .build();

    private final Cache<String, Bucket> registerBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(20))
            .maximumSize(10_000)
            .build();

    private final Cache<String, Bucket> metricsBuckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(2))
            .maximumSize(10_000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(request);
        Bucket bucket = resolveBucket(request.getRequestURI(), ip);

        if (bucket == null) {
            chain.doFilter(request, response);
            return;
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setHeader("X-Rate-Limit-Retry-After-Seconds",
                    String.valueOf(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests\",\"status\":429}");
        }
    }

    private Bucket resolveBucket(String path, String ip) {
        return switch (path) {
            case "/api/auth/login" -> loginBuckets.get(ip, k -> Bucket.builder()
                    .addLimit(limit -> limit.capacity(10).refillGreedy(10, Duration.ofMinutes(1)))
                    .build());
            case "/api/auth/register" -> registerBuckets.get(ip, k -> Bucket.builder()
                    .addLimit(limit -> limit.capacity(5).refillGreedy(5, Duration.ofMinutes(15)))
                    .build());
            case "/api/metrics" -> metricsBuckets.get(ip, k -> Bucket.builder()
                    .addLimit(limit -> limit.capacity(30).refillGreedy(30, Duration.ofMinutes(1)))
                    .build());
            default -> null;
        };
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
