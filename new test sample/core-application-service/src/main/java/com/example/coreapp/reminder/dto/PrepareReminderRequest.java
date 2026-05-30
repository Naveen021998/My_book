package com.example.coreapp.reminder.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PrepareReminderRequest(
        @Pattern(regexp = "EMAIL|SMS|PUSH", message = "channel must be EMAIL, SMS, or PUSH")
        String channel,
        @Size(max = 160, message = "destination must be at most 160 characters")
        String destination,
        @Size(max = 120, message = "title must be at most 120 characters")
        String title,
        @Size(max = 1000, message = "body must be at most 1000 characters")
        String body
) {
}
