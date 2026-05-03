package com.example.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(
        @Email(message = "email must be valid")
        @NotBlank(message = "email is required")
        @Size(max = 160, message = "email must be at most 160 characters")
        String email,
        @NotBlank(message = "otpCode is required")
        @Size(min = 6, max = 6, message = "otpCode must be 6 digits")
        String otpCode
) {
}
