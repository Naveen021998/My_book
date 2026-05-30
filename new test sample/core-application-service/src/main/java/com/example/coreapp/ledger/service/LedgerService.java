package com.example.coreapp.ledger.service;

import com.example.coreapp.audit.service.AuditLogService;
import com.example.coreapp.common.exception.ResourceNotFoundException;
import com.example.coreapp.common.web.PagedResponse;
import com.example.coreapp.ledger.dto.CreateLedgerEntryRequest;
import com.example.coreapp.ledger.dto.LedgerEntryResponse;
import com.example.coreapp.ledger.entity.LedgerEntryEntity;
import com.example.coreapp.ledger.mapper.LedgerEntryMapper;
import com.example.coreapp.ledger.repository.LedgerEntryRepository;
import com.example.coreapp.party.entity.PartyBalanceEntity;
import com.example.coreapp.party.entity.PartyEntity;
import com.example.coreapp.party.repository.PartyBalanceRepository;
import com.example.coreapp.party.repository.PartyRepository;
import com.example.coreapp.security.CurrentUser;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

    private final PartyRepository partyRepository;
    private final PartyBalanceRepository partyBalanceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerEntryMapper ledgerEntryMapper;
    private final CurrentUser currentUser;
    private final AuditLogService auditLogService;

    public LedgerService(
            PartyRepository partyRepository,
            PartyBalanceRepository partyBalanceRepository,
            LedgerEntryRepository ledgerEntryRepository,
            LedgerEntryMapper ledgerEntryMapper,
            CurrentUser currentUser,
            AuditLogService auditLogService
    ) {
        this.partyRepository = partyRepository;
        this.partyBalanceRepository = partyBalanceRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.ledgerEntryMapper = ledgerEntryMapper;
        this.currentUser = currentUser;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public LedgerEntryResponse createEntry(long partyId, CreateLedgerEntryRequest request, String idempotencyKey) {
        long userId = currentUser.userId();

        PartyEntity party = partyRepository.findByIdAndOwnerUserId(partyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found."));

        String normalizedIdempotencyKey = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
        if (normalizedIdempotencyKey != null) {
            LedgerEntryEntity existing = ledgerEntryRepository.findByOwnerUserIdAndIdempotencyKey(userId, normalizedIdempotencyKey)
                    .orElse(null);
            if (existing != null) {
                return ledgerEntryMapper.toResponse(existing);
            }
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // Lock balance row to make entry+balance update atomic per party.
        PartyBalanceEntity balance = partyBalanceRepository.lockByPartyIdAndOwnerUserId(partyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Party balance not found."));

        long delta = computeDelta(request.direction(), request.amountMinor());
        balance.setBalanceMinor(balance.getBalanceMinor() + delta);
        balance.setUpdatedAt(now);

        LedgerEntryEntity entry = new LedgerEntryEntity();
        entry.setOwnerUserId(userId);
        entry.setPartyId(party.getId());
        entry.setDirection(request.direction().trim().toUpperCase());
        entry.setAmountMinor(request.amountMinor());
        entry.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        entry.setEntryDate(request.entryDate());
        entry.setIdempotencyKey(normalizedIdempotencyKey);
        entry.setCreatedAt(now);

        LedgerEntryEntity saved = ledgerEntryRepository.save(entry);

        auditLogService.log(
                "LEDGER_ENTRY_CREATE",
                "LEDGER_ENTRY",
                saved.getId(),
                Map.of(
                        "partyId", partyId,
                        "direction", saved.getDirection(),
                        "amountMinor", saved.getAmountMinor(),
                        "entryDate", String.valueOf(saved.getEntryDate()),
                        "idempotencyKey", saved.getIdempotencyKey()
                )
        );

        return ledgerEntryMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<LedgerEntryResponse> getEntries(long partyId, int page, int size) {
        long userId = currentUser.userId();
        partyRepository.findByIdAndOwnerUserId(partyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found."));

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                clampSize(size),
                Sort.unsorted()
        );

        Page<LedgerEntryEntity> entries = ledgerEntryRepository
                .findByOwnerUserIdAndPartyIdOrderByEntryDateDescIdDesc(userId, partyId, pageable);

        return new PagedResponse<>(
                entries.getContent().stream().map(ledgerEntryMapper::toResponse).toList(),
                entries.getTotalElements(),
                entries.getTotalPages(),
                entries.getNumber(),
                entries.getSize(),
                entries.hasNext()
        );
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 200);
    }

    private long computeDelta(String direction, long amountMinor) {
        String normalized = direction == null ? "" : direction.trim().toUpperCase();
        if ("CREDIT".equals(normalized)) {
            return amountMinor;
        }
        if ("DEBIT".equals(normalized)) {
            return -amountMinor;
        }
        throw new IllegalArgumentException("direction must be CREDIT or DEBIT");
    }
}
