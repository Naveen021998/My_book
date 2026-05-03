package com.example.apigateway.web;

import com.example.apigateway.config.UpstreamProperties;
import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

@Controller
public class GatewayController {

    private static final List<String> HOP_BY_HOP_HEADERS = List.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade"
    );

    private final UpstreamProperties upstreams;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String internalToken;

    public GatewayController(
            UpstreamProperties upstreams,
            ObjectMapper objectMapper,
            @Value("${app.internal.token:}") String internalToken
    ) {
        this.upstreams = upstreams;
        this.objectMapper = objectMapper;
        this.internalToken = internalToken;
        this.webClient = WebClient.builder()
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(
                        HttpClient.create()
                                .compress(true)
                                .followRedirect(true)
                                .responseTimeout(Duration.ofSeconds(30))
                ))
                // Avoid buffering large payloads in memory (file uploads/downloads).
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build())
                .build();
    }

    @RequestMapping(path = "/health", method = {RequestMethod.GET})
    public Mono<Void> health(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.OK);
        exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
        byte[] bytes = "OK\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        DataBuffer buf = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buf));
    }

    @RequestMapping(
            path = {"/api/v1/**", "/swagger/**"},
            method = {
                    RequestMethod.GET, RequestMethod.HEAD, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH,
                    RequestMethod.DELETE, RequestMethod.OPTIONS
            }
    )
    public Mono<Void> proxy(ServerWebExchange exchange) {
        URI target = resolveTarget(exchange.getRequest());
        if (target == null) {
            return writeJsonError(exchange.getResponse(), HttpStatus.NOT_FOUND, "NOT_FOUND", "Not Found");
        }

        // Special-case: register should also provision the user profile (best-effort).
        if (isRegister(exchange.getRequest())) {
            return forwardRegisterWithProvision(exchange, target)
                    .onErrorResume(ConnectException.class, ex ->
                            writeJsonError(exchange.getResponse(), HttpStatus.BAD_GATEWAY, "UPSTREAM_DOWN",
                                    "Service is temporarily unavailable. Please try again."))
                    .onErrorResume(Exception.class, ex ->
                            writeJsonError(exchange.getResponse(), HttpStatus.BAD_GATEWAY, "GATEWAY_ERROR",
                                    "Gateway error. Please try again."));
        }

        return forward(exchange, target)
                .onErrorResume(ConnectException.class, ex ->
                        writeJsonError(exchange.getResponse(), HttpStatus.BAD_GATEWAY, "UPSTREAM_DOWN",
                                "Service is temporarily unavailable. Please try again."))
                .onErrorResume(UnsupportedMediaTypeStatusException.class, ex ->
                        writeJsonError(exchange.getResponse(), HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                                "Unsupported content type."))
                .onErrorResume(Exception.class, ex ->
                        writeJsonError(exchange.getResponse(), HttpStatus.BAD_GATEWAY, "GATEWAY_ERROR",
                                "Gateway error. Please try again."));
    }

    private boolean isRegister(ServerHttpRequest request) {
        if (request.getMethod() != HttpMethod.POST) {
            return false;
        }
        String path = request.getURI().getRawPath();
        return "/api/v1/auth/register".equals(path);
    }

    private Mono<Void> forwardRegisterWithProvision(ServerWebExchange exchange, URI target) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        if (method == null) {
            return writeJsonError(exchange.getResponse(), HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed.");
        }

        WebClient.RequestBodySpec spec = webClient.method(method)
                .uri(target);

        HttpHeaders outboundHeaders = new HttpHeaders();
        request.getHeaders().forEach((name, values) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            if (HOP_BY_HOP_HEADERS.contains(lower)) {
                return;
            }
            if (HttpHeaders.HOST.equalsIgnoreCase(name)) {
                return;
            }
            outboundHeaders.put(name, values);
        });
        outboundHeaders.set("X-Forwarded-Proto", request.getURI().getScheme());
        String remoteAddr = request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : null;
        if (StringUtils.hasText(remoteAddr)) {
            outboundHeaders.set("X-Real-IP", remoteAddr);
            String existingXff = request.getHeaders().getFirst("X-Forwarded-For");
            outboundHeaders.set("X-Forwarded-For", StringUtils.hasText(existingXff) ? (existingXff + ", " + remoteAddr) : remoteAddr);
        }
        spec.headers(h -> h.addAll(outboundHeaders));

        return spec.body(BodyInserters.fromDataBuffers(request.getBody()))
                .exchangeToMono(resp -> resp.bodyToMono(byte[].class)
                        .defaultIfEmpty(new byte[0])
                        .flatMap(bytes -> provisionUserFromRegisterResponse(resp, bytes)
                                .then(writeBufferedResponse(exchange.getResponse(), resp, bytes))));
    }

    private Mono<Void> provisionUserFromRegisterResponse(ClientResponse authResponse, byte[] bodyBytes) {
        if (!authResponse.statusCode().is2xxSuccessful() || bodyBytes.length == 0) {
            return Mono.empty();
        }
        if (!StringUtils.hasText(internalToken)) {
            return Mono.empty();
        }

        AuthTokensResponseLite tokens;
        try {
            tokens = objectMapper.readValue(bodyBytes, new TypeReference<AuthTokensResponseLite>() {});
        } catch (Exception e) {
            return Mono.empty();
        }

        if (tokens.user() == null || tokens.user().id() == null) {
            return Mono.empty();
        }

        ProvisionUserRequest provision = new ProvisionUserRequest(tokens.user().id(), tokens.user().fullName(), tokens.user().email());

        return webClient.post()
                .uri(upstreams.user() + "/api/v1/users/internal/provision")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Token", internalToken)
                .bodyValue(provision)
                // Best-effort: do not block auth register response if provisioning fails.
                .exchangeToMono(r -> r.bodyToMono(Void.class))
                .onErrorResume(ex -> Mono.empty());
    }

    private Mono<Void> writeBufferedResponse(ServerHttpResponse response, ClientResponse upstream, byte[] bodyBytes) {
        response.setStatusCode(upstream.statusCode());
        upstream.headers().asHttpHeaders().forEach((name, values) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            if (HOP_BY_HOP_HEADERS.contains(lower)) {
                return;
            }
            response.getHeaders().put(name, values);
        });
        DataBuffer buf = response.bufferFactory().wrap(bodyBytes);
        return response.writeWith(Mono.just(buf));
    }

    private record AuthUserLite(Long id, String fullName, String email) {}
    private record AuthTokensResponseLite(AuthUserLite user) {}
    private record ProvisionUserRequest(Long authUserId, String fullName, String email) {}

    private Mono<Void> forward(ServerWebExchange exchange, URI target) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        if (method == null) {
            return writeJsonError(exchange.getResponse(), HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed.");
        }

        WebClient.RequestBodySpec spec = webClient.method(method).uri(target);

        HttpHeaders outboundHeaders = new HttpHeaders();
        request.getHeaders().forEach((name, values) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            if (HOP_BY_HOP_HEADERS.contains(lower)) {
                return;
            }
            if (HttpHeaders.HOST.equalsIgnoreCase(name)) {
                return;
            }
            outboundHeaders.put(name, values);
        });
        // Add / override common reverse-proxy headers.
        outboundHeaders.set("X-Forwarded-Proto", request.getURI().getScheme());
        String remoteAddr = request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : null;
        if (StringUtils.hasText(remoteAddr)) {
            outboundHeaders.set("X-Real-IP", remoteAddr);
            String existingXff = request.getHeaders().getFirst("X-Forwarded-For");
            outboundHeaders.set("X-Forwarded-For", StringUtils.hasText(existingXff) ? (existingXff + ", " + remoteAddr) : remoteAddr);
        }
        spec.headers(h -> h.addAll(outboundHeaders));

        // Stream request body if present.
        if (method == HttpMethod.GET || method == HttpMethod.HEAD) {
            return spec.exchangeToMono(resp -> writeResponse(exchange.getResponse(), resp));
        }

        return spec.body(BodyInserters.fromDataBuffers(request.getBody()))
                .exchangeToMono(resp -> writeResponse(exchange.getResponse(), resp));
    }

    private Mono<Void> writeResponse(ServerHttpResponse response, ClientResponse upstream) {
        response.setStatusCode(upstream.statusCode());
        upstream.headers().asHttpHeaders().forEach((name, values) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            if (HOP_BY_HOP_HEADERS.contains(lower)) {
                return;
            }
            response.getHeaders().put(name, values);
        });
        return response.writeWith(upstream.bodyToFlux(DataBuffer.class));
    }

    private URI resolveTarget(ServerHttpRequest request) {
        String rawPath = request.getURI().getRawPath();
        String rawQuery = request.getURI().getRawQuery();

        String base;
        String path = rawPath;

        if (rawPath.startsWith("/api/v1/auth")) {
            base = upstreams.auth();
        } else if (rawPath.startsWith("/api/v1/users")) {
            base = upstreams.user();
        } else if (rawPath.startsWith("/api/v1/notifications")) {
            base = upstreams.notification();
        } else if (rawPath.startsWith("/api/v1/files")) {
            base = upstreams.file();
        } else if (rawPath.startsWith("/api/v1/")) {
            base = upstreams.core();
        } else if (rawPath.startsWith("/swagger/auth/")) {
            base = upstreams.auth();
            path = "/" + rawPath.substring("/swagger/auth/".length());
        } else if (rawPath.startsWith("/swagger/users/")) {
            base = upstreams.user();
            path = "/" + rawPath.substring("/swagger/users/".length());
        } else if (rawPath.startsWith("/swagger/core/")) {
            base = upstreams.core();
            path = "/" + rawPath.substring("/swagger/core/".length());
        } else if (rawPath.startsWith("/swagger/notifications/")) {
            base = upstreams.notification();
            path = "/" + rawPath.substring("/swagger/notifications/".length());
        } else if (rawPath.startsWith("/swagger/files/")) {
            base = upstreams.file();
            path = "/" + rawPath.substring("/swagger/files/".length());
        } else {
            return null;
        }

        if (!StringUtils.hasText(base)) {
            return null;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(base).path(path);
        if (StringUtils.hasText(rawQuery)) {
            builder.query(rawQuery);
        }
        return builder.build(true).toUri();
    }

    private Mono<Void> writeJsonError(ServerHttpResponse response, HttpStatus status, String code, String message) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String payload = toJson(new ErrorResponse(code, message, OffsetDateTime.now(ZoneOffset.UTC)));
        DataBuffer buf = response.bufferFactory().wrap(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buf));
    }

    // Minimal JSON writer to avoid adding dependencies.
    private static String toJson(ErrorResponse error) {
        return "{"
                + "\"code\":\"" + escape(error.code()) + "\","
                + "\"message\":\"" + escape(error.message()) + "\","
                + "\"timestamp\":\"" + error.timestamp().toString() + "\""
                + "}";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
