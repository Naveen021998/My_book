package com.example.coreapp.reminder.dto;

public record PreparedReminderResponse(
        ReminderResponse reminder,
        NotificationSendDraft notification
) {
}
