package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.regex.Pattern;

public record LoginRequest(
        @NotBlank(message = "phoneNumber is required")
        String phoneNumber,

        @NotBlank(message = "password is required")
        String password
) {
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    public LoginRequest {
        String cleaned = phoneNumber.trim();

        // Auto-prepend +91 for 10-digit local Indian numbers
        if (!cleaned.startsWith("+")) {
            String digitsOnly = cleaned.replaceAll("\\D", "");
            if (digitsOnly.length() == 10 && digitsOnly.matches("^[6-9]\\d{9}$")) {
                cleaned = "+91" + digitsOnly;
            } else {
                cleaned = "+" + digitsOnly;
            }
        }

        if (!E164_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("phoneNumber must be valid E.164 format");
        }

        phoneNumber = cleaned;
    }
}
