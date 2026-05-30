package com.example.authservice.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int LIMIT_PER_WINDOW = 30;
    private static final Set<String> PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh"
    );

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp(request) + "|" + path;
        WindowCounter counter = counters.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || existing.isExpired(now)) {
                return new WindowCounter(now);
            }
            return existing;
        });

        int count = counter.incrementAndGet();
        if (count > LIMIT_PER_WINDOW) {
            long retryAfterSeconds = Math.max(1, counter.retryAfterSeconds());
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getOutputStream().write(
                    ("{\"message\":\"Too many requests. Try again later.\"}")
                            .getBytes(StandardCharsets.UTF_8)
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int comma = forwardedFor.indexOf(',');
            return (comma >= 0 ? forwardedFor.substring(0, comma) : forwardedFor).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static final class WindowCounter {
        private final long windowStartMillis;
        private final AtomicInteger count;

        private WindowCounter(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
            this.count = new AtomicInteger(0);
        }

        private boolean isExpired(long nowMillis) {
            return nowMillis - windowStartMillis >= WINDOW.toMillis();
        }

        private int incrementAndGet() {
            return count.incrementAndGet();
        }

        private long retryAfterSeconds() {
            long now = System.currentTimeMillis();
            long remaining = WINDOW.toMillis() - (now - windowStartMillis);
            return (long) Math.ceil(Math.max(0, remaining) / 1000.0);
        }
    }
}
