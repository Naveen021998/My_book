package com.example.coreapp.reminder.controller;

import com.example.coreapp.common.web.PagedResponse;
import com.example.coreapp.reminder.dto.PrepareReminderRequest;
import com.example.coreapp.reminder.dto.PreparedReminderResponse;
import com.example.coreapp.reminder.dto.ReminderResponse;
import com.example.coreapp.reminder.service.ReminderService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1")
public class PartyReminderController {

    private final ReminderService reminderService;

    public PartyReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping("/parties/{partyId}/reminders")
    public ResponseEntity<PreparedReminderResponse> prepare(
            @PathVariable long partyId,
            @Valid @RequestBody PrepareReminderRequest request
    ) {
        PreparedReminderResponse response = reminderService.prepare(partyId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .replacePath("/api/v1/reminders")
                .path("/{id}")
                .buildAndExpand(response.reminder().id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/parties/{partyId}/reminders")
    public ResponseEntity<PagedResponse<ReminderResponse>> list(
            @PathVariable long partyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(reminderService.listForParty(partyId, page, size));
    }

    @PostMapping("/reminders/{reminderId}/sent")
    public ResponseEntity<ReminderResponse> markSent(@PathVariable long reminderId) {
        return ResponseEntity.ok(reminderService.markSent(reminderId));
    }
}
