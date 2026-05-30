package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.regex.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "phoneNumber is required")
        @Size(min = 10, max = 15, message = "phoneNumber must be between 10 and 15 digits")
        String phoneNumber,

        @NotBlank(message = "otpCode is required")
        @Size(min = 6, max = 6, message = "otpCode must be 6 digits")
        String otpCode
) {
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    public VerifyOtpRequest {
        String cleaned = phoneNumber.trim();

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
