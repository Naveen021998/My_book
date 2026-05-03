package com.example.userservice.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Internal-only API used by the gateway/auth flow to provision a minimal user profile
 * when an auth account is created.
 */
public record ProvisionUserRequest(
        @NotNull(message = "authUserId is required")
        Long authUserId,
        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be at most 120 characters")
        String fullName,
        @Email(message = "email must be valid")
        @NotBlank(message = "email is required")
        @Size(max = 160, message = "email must be at most 160 characters")
        String email
) {
}

