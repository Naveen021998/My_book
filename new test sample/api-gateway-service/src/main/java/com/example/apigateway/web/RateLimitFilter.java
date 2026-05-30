package com.example.apigateway.web;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class RateLimitFilter implements WebFilter {

    private final Duration window;
    private final int limitPerWindow;
    private final String pathPrefix;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.rate-limit.window-seconds:60}") int windowSeconds,
            @Value("${app.rate-limit.limit:240}") int limitPerWindow,
            @Value("${app.rate-limit.path-prefix:/api/}") String pathPrefix) {
        this.window = Duration.ofSeconds(windowSeconds);
        this.limitPerWindow = limitPerWindow;
        this.pathPrefix = pathPrefix;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path == null || !path.startsWith(pathPrefix)) {
            return chain.filter(exchange);
        }

        String key = clientIp(exchange.getRequest()) + "|" + path;
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
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(retryAfterSeconds));
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String body = "{\"message\":\"Too many requests. Try again later.\"}";
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8)))
            );
        }

        return chain.filter(exchange);
    }

    private String clientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int comma = forwardedFor.indexOf(',');
            return (comma >= 0 ? forwardedFor.substring(0, comma) : forwardedFor).trim();
        }
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
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

