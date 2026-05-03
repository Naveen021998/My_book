package com.example.notificationservice.notification.dto;

import java.time.OffsetDateTime;

public record NotificationResponse(
        long id,
        String channel,
        String destination,
        String title,
        String body,
        String status,
        OffsetDateTime createdAt
) {
}

