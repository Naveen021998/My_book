package com.example.coreapp.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Duration window;
    private final int limitPerWindow;
    private final String pathPrefix;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.rate-limit.window-seconds:60}") int windowSeconds,
            @Value("${app.rate-limit.limit:120}") int limitPerWindow,
            @Value("${app.rate-limit.path-prefix:/api/}") String pathPrefix) {
        this.window = Duration.ofSeconds(windowSeconds);
        this.limitPerWindow = limitPerWindow;
        this.pathPrefix = pathPrefix;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith(pathPrefix)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp(request) + "|" + path;
        WindowCounter counter = counters.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || existing.isExpired(now, window)) {
                return new WindowCounter(now);
            }
            return existing;
        });

        int count = counter.incrementAndGet();
        if (count > limitPerWindow) {
            long retryAfterSeconds = Math.max(1, counter.retryAfterSeconds(window));
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

        private boolean isExpired(long nowMillis, Duration window) {
            return nowMillis - windowStartMillis >= window.toMillis();
        }

        private int incrementAndGet() {
            return count.incrementAndGet();
        }

        private long retryAfterSeconds(Duration window) {
            long now = System.currentTimeMillis();
            long remaining = window.toMillis() - (now - windowStartMillis);
            return (long) Math.ceil(Math.max(0, remaining) / 1000.0);
        }
    }
}
