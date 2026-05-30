package com.example.coreapp.party.service;

import com.example.coreapp.audit.service.AuditLogService;
import com.example.coreapp.common.exception.ResourceNotFoundException;
import com.example.coreapp.common.web.PagedResponse;
import com.example.coreapp.ledger.repository.LedgerEntryRepository;
import com.example.coreapp.party.dto.CreatePartyRequest;
import com.example.coreapp.party.dto.PartyResponse;
import com.example.coreapp.party.dto.UpdatePartyRequest;
import com.example.coreapp.party.entity.PartyBalanceEntity;
import com.example.coreapp.party.entity.PartyEntity;
import com.example.coreapp.party.mapper.PartyMapper;
import com.example.coreapp.party.repository.PartyBalanceRepository;
import com.example.coreapp.party.repository.PartyRepository;
import com.example.coreapp.reminder.repository.ReminderRepository;
import com.example.coreapp.security.CurrentUser;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartyService {

    private final PartyRepository partyRepository;
    private final PartyBalanceRepository partyBalanceRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ReminderRepository reminderRepository;
    private final PartyMapper partyMapper;
    private final CurrentUser currentUser;
    private final AuditLogService auditLogService;

    public PartyService(
            PartyRepository partyRepository,
            PartyBalanceRepository partyBalanceRepository,
            LedgerEntryRepository ledgerEntryRepository,
            ReminderRepository reminderRepository,
            PartyMapper partyMapper,
            CurrentUser currentUser,
            AuditLogService auditLogService
    ) {
        this.partyRepository = partyRepository;
        this.partyBalanceRepository = partyBalanceRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.reminderRepository = reminderRepository;
        this.partyMapper = partyMapper;
        this.currentUser = currentUser;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PartyResponse createParty(CreatePartyRequest request) {
        long userId = currentUser.userId();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        PartyEntity party = new PartyEntity();
        party.setOwnerUserId(userId);
        party.setName(request.name().trim());
        party.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        party.setType(request.type().trim().toUpperCase());
        party.setCreatedAt(now);
        party.setUpdatedAt(now);

        PartyEntity saved = partyRepository.save(party);

        PartyBalanceEntity balance = new PartyBalanceEntity();
        balance.setPartyId(saved.getId());
        balance.setOwnerUserId(userId);
        balance.setBalanceMinor(0L);
        balance.setUpdatedAt(now);
        partyBalanceRepository.save(balance);

        auditLogService.log(
                "PARTY_CREATE",
                "PARTY",
                saved.getId(),
                Map.of(
                        "name", saved.getName(),
                        "type", saved.getType(),
                        "phone", saved.getPhone()
                )
        );

        return partyMapper.toResponse(saved, balance);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PartyResponse> getParties(int page, int size, String query) {
        long userId = currentUser.userId();
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                clampSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<PartyEntity> parties = partyRepository.search(userId, normalizeQuery(query), pageable);
        List<Long> ids = parties.getContent().stream().map(PartyEntity::getId).toList();
        Map<Long, PartyBalanceEntity> balancesByPartyId = partyBalanceRepository.findByPartyIdIn(ids).stream()
                .collect(Collectors.toMap(PartyBalanceEntity::getPartyId, Function.identity()));

        List<PartyResponse> items = parties.getContent().stream()
                .map(p -> partyMapper.toResponse(p, balancesByPartyId))
                .toList();

        return new PagedResponse<>(
                items,
                parties.getTotalElements(),
                parties.getTotalPages(),
                parties.getNumber(),
                parties.getSize(),
                parties.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public PartyResponse getParty(long partyId) {
        long userId = currentUser.userId();
        PartyEntity party = partyRepository.findByIdAndOwnerUserId(partyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found."));
        PartyBalanceEntity balance = partyBalanceRepository.findByPartyIdAndOwnerUserId(partyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Party balance not found."));
        return partyMapper.toResponse(party, balance);
    }

    @Transactional
    public PartyResponse updateParty(long partyId, UpdatePartyRequest request) {
        long userId = currentUser.userId();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        PartyEntity party = partyRepository.findByIdAndOwnerUserId(partyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found."));

        boolean changed = false;

        if (request.name() != null) {
            party.setName(request.name().trim());
            changed = true;
        }
        if (request.phone() != null) {
            String phone = request.phone().isBlank() ? null : request.phone().trim();
            party.setPhone(phone);
            changed = true;
        }
        if (request.type() != null) {
            party.setType(request.type().trim().toUpperCase());
            changed = true;
        }
        party.setUpdatedAt(now);

        PartyBalanceEntity balance = partyBalanceRepository.findByPartyIdAndOwnerUserId(partyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Party balance not found."));
        balance.setUpdatedAt(now);

        if (changed) {
            auditLogService.log(
                    "PARTY_UPDATE",
                    "PARTY",
                    party.getId(),
                    Map.of(
                            "name", party.getName(),
                            "type", party.getType(),
                            "phone", party.getPhone()
                    )
            );
        }

        return partyMapper.toResponse(party, balance);
    }

    @Transactional
    public void deleteParty(long partyId) {
        long userId = currentUser.userId();
        PartyEntity party = partyRepository.findByIdAndOwnerUserId(partyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found."));

        reminderRepository.deleteByOwnerUserIdAndPartyId(userId, partyId);
        ledgerEntryRepository.deleteByOwnerUserIdAndPartyId(userId, partyId);
        partyBalanceRepository.deleteByOwnerUserIdAndPartyId(userId, partyId);
        partyRepository.deleteByIdAndOwnerUserId(partyId, userId);

        auditLogService.log(
                "PARTY_DELETE",
                "PARTY",
                party.getId(),
                Map.of("name", party.getName())
        );
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
