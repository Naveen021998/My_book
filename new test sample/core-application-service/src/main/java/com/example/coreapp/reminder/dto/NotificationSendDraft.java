package com.example.coreapp.reminder.dto;

public record NotificationSendDraft(
        String channel,
        String destination,
        String title,
        String body
) {
}
