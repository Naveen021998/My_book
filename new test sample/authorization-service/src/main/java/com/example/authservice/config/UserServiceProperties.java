package com.example.authservice.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.user-service")
public record UserServiceProperties(@NotBlank String baseUrl) {
}
