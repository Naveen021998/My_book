package com.example.userservice.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
        @Size(max = 120, message = "fullName must be at most 120 characters")
        String fullName,
        @Size(max = 30, message = "phoneNumber must be at most 30 characters")
        @Pattern(regexp = "^[0-9+\\-() ]{7,30}$", message = "phoneNumber format is invalid")
        String phoneNumber
) {
}

