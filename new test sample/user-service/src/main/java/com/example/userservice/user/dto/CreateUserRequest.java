package com.example.userservice.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be at most 120 characters")
        String fullName,
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 160, message = "email must be at most 160 characters")
        String email,
        @NotBlank(message = "phoneNumber is required")
        @Pattern(regexp = "^[0-9+\\-() ]{7,30}$", message = "phoneNumber format is invalid")
        String phoneNumber
) {
}

