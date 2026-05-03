package com.example.userservice.security;

import com.example.userservice.common.api.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Simple internal auth mechanism for service-to-service calls during early development.
 *
 * Protects `/api/v1/users/internal/**` using a shared secret header.
 */
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Token";

    private final String expectedToken;
    private final ObjectMapper objectMapper;

    public InternalTokenFilter(String expectedToken, ObjectMapper objectMapper) {
        this.expectedToken = expectedToken;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/v1/users/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(expectedToken) || !StringUtils.hasText(token) || !expectedToken.equals(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiErrorResponse payload = new ApiErrorResponse(
                    "UNAUTHORIZED",
                    "Internal token is missing or invalid.",
                    List.of(),
                    OffsetDateTime.now(ZoneOffset.UTC)
            );
            response.getWriter().write(objectMapper.writeValueAsString(payload));
            return;
        }

        filterChain.doFilter(request, response);
    }
}

