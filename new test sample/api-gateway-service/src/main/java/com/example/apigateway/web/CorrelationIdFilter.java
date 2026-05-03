package com.example.apigateway.web;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Propagate to downstream and back to client.
        String finalCorrelationId = correlationId;
        ServerWebExchange mutated = exchange.mutate()
                .request(req -> req.headers(h -> h.set(HEADER, finalCorrelationId)))
                .build();
        mutated.getResponse().getHeaders().set(HEADER, finalCorrelationId);
        return chain.filter(mutated);
    }
}
