package com.example.notificationservice.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendNotificationRequest(
        @NotBlank
        @Pattern(regexp = "EMAIL|SMS|PUSH", message = "channel must be EMAIL, SMS, or PUSH")
        String channel,
        @NotBlank @Size(max = 160) String destination,
        @Size(max = 120) String title,
        @NotBlank @Size(max = 1000) String body
) {
}

