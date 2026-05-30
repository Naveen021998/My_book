package com.example.coreapp.reminder.service;

import com.example.coreapp.audit.service.AuditLogService;
import com.example.coreapp.common.exception.ResourceNotFoundException;
import com.example.coreapp.common.web.PagedResponse;
import com.example.coreapp.party.entity.PartyBalanceEntity;
import com.example.coreapp.party.entity.PartyEntity;
import com.example.coreapp.party.repository.PartyBalanceRepository;
import com.example.coreapp.party.repository.PartyRepository;
import com.example.coreapp.reminder.dto.NotificationSendDraft;
import com.example.coreapp.reminder.dto.PrepareReminderRequest;
import com.example.coreapp.reminder.dto.PreparedReminderResponse;
import com.example.coreapp.reminder.dto.ReminderResponse;
import com.example.coreapp.reminder.entity.ReminderEntity;
import com.example.coreapp.reminder.repository.ReminderRepository;
import com.example.coreapp.security.CurrentUser;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final PartyRepository partyRepository;
    private final PartyBalanceRepository partyBalanceRepository;
    private final CurrentUser currentUser;
    private final AuditLogService auditLogService;

    public ReminderService(
            ReminderRepository reminderRepository,
            PartyRepository partyRepository,
            PartyBalanceRepository partyBalanceRepository,
            CurrentUser currentUser,
            AuditLogService auditLogService
    ) {
        this.reminderRepository = reminderRepository;
        this.partyRepository = partyRepository;
        this.partyBalanceRepository = partyBalanceRepository;
        this.currentUser = currentUser;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PreparedReminderResponse prepare(long partyId, PrepareReminderRequest request) {
        long userId = currentUser.userId();
        PartyEntity party = partyRepository.findByIdAndOwnerUserId(partyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found."));
        PartyBalanceEntity balance = partyBalanceRepository.findByPartyIdAndOwnerUserId(partyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Party balance not found."));

        String channel = normalize(request.channel(), "SMS");
        String destination = normalize(request.destination(), party.getPhone());
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("destination is required (party phone is missing)");
        }

        String title = normalize(request.title(), "Payment Reminder");
        String body = normalize(request.body(), defaultBody(party.getName(), balance.getBalanceMinor()));

        ReminderEntity reminder = new ReminderEntity();
        reminder.setOwnerUserId(userId);
        reminder.setPartyId(partyId);
        reminder.setChannel(channel);
        reminder.setDestination(destination);
        reminder.setTitle(title);
        reminder.setBody(body);
        reminder.setStatus("PREPARED");

        ReminderEntity saved = reminderRepository.save(reminder);

        auditLogService.log(
                "REMINDER_PREPARE",
                "REMINDER",
                saved.getId(),
                Map.of(
                        "partyId", partyId,
                        "channel", channel,
                        "destination", destination
                )
        );

        return new PreparedReminderResponse(
                toResponse(saved),
                new NotificationSendDraft(channel, destination, title, body)
        );
    }

    @Transactional
    public ReminderResponse markSent(long reminderId) {
        long userId = currentUser.userId();
        ReminderEntity reminder = reminderRepository.findByOwnerUserIdAndId(userId, reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found."));

        reminder.setStatus("SENT");
        reminder.setSentAt(OffsetDateTime.now(ZoneOffset.UTC));

        auditLogService.log(
                "REMINDER_SENT",
                "REMINDER",
                reminder.getId(),
                Map.of(
                        "partyId", reminder.getPartyId(),
                        "channel", reminder.getChannel(),
                        "destination", reminder.getDestination()
                )
        );

        return toResponse(reminder);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ReminderResponse> listForParty(long partyId, int page, int size) {
        long userId = currentUser.userId();
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                clampSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<ReminderEntity> reminders = reminderRepository.findByOwnerUserIdAndPartyIdOrderByCreatedAtDescIdDesc(userId, partyId, pageable);
        List<ReminderResponse> items = reminders.getContent().stream().map(this::toResponse).toList();

        return new PagedResponse<>(
                items,
                reminders.getTotalElements(),
                reminders.getTotalPages(),
                reminders.getNumber(),
                reminders.getSize(),
                reminders.hasNext()
        );
    }

    private String defaultBody(String partyName, long balanceMinor) {
        double amount = Math.abs(balanceMinor) / 100.0;
        if (balanceMinor > 0) {
            return "Hi " + partyName + ", reminder: you have a pending balance of Rs " + String.format("%.2f", amount) + ".";
        }
        if (balanceMinor < 0) {
            return "Hi " + partyName + ", reminder: we have a pending balance of Rs " + String.format("%.2f", amount) + ".";
        }
        return "Hi " + partyName + ", reminder: please check your ledger balance.";
    }

    private String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? fallback : trimmed;
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private ReminderResponse toResponse(ReminderEntity e) {
        return new ReminderResponse(
                e.getId(),
                e.getPartyId(),
                e.getChannel(),
                e.getDestination(),
                e.getTitle(),
                e.getBody(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getSentAt()
        );
    }
}
