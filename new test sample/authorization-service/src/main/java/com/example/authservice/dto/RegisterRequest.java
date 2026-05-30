package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.regex.Pattern;

public record RegisterRequest(
        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be at most 120 characters")
        String fullName,

        @NotBlank(message = "phoneNumber is required")
        @Size(min = 10, max = 15, message = "phoneNumber must be between 10 and 15 digits (E.164 format: +[country][number])")
        String phoneNumber,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
        String password
) {
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    public RegisterRequest {
        // Normalize: trim whitespace
        String cleaned = phoneNumber.trim();

        // Auto-prepend +91 if no + and looks like local Indian number (10 digits starting 6-9)
        if (!cleaned.startsWith("+")) {
            String digitsOnly = cleaned.replaceAll("\\D", "");
            if (digitsOnly.length() == 10 && digitsOnly.matches("^[6-9]\\d{9}$")) {
                cleaned = "+91" + digitsOnly;
            } else {
                cleaned = "+" + digitsOnly;
            }
        }

        // Validate E.164: +[country code (1-3 digits)][number (max 14 digits total after +)]
        // Total length: 10-15 including +
        if (!E164_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("phoneNumber must be valid E.164 format (e.g., +919876543210)");
        }

        // Replace original with normalized
        phoneNumber = cleaned;
    }
}

