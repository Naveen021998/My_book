package com.example.coreapp.reminder.dto;

import java.time.OffsetDateTime;

public record ReminderResponse(
        long id,
        long partyId,
        String channel,
        String destination,
        String title,
        String body,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime sentAt
) {
}
