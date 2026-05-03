package com.example.coreapp.party.controller;

import com.example.coreapp.common.exception.ResourceNotFoundException;
import com.example.coreapp.ledger.entity.LedgerEntryEntity;
import com.example.coreapp.ledger.repository.LedgerEntryRepository;
import com.example.coreapp.party.entity.PartyEntity;
import com.example.coreapp.party.repository.PartyRepository;
import com.example.coreapp.security.CurrentUser;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parties")
public class PartyExportController {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PartyRepository partyRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final CurrentUser currentUser;

    public PartyExportController(
            PartyRepository partyRepository,
            LedgerEntryRepository ledgerEntryRepository,
            CurrentUser currentUser
    ) {
        this.partyRepository = partyRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.currentUser = currentUser;
    }

    @GetMapping(value = "/{partyId}/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable long partyId) {
        long userId = currentUser.userId();
        PartyEntity party = partyRepository.findByIdAndOwnerUserId(partyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Party not found."));

        List<LedgerEntryEntity> entries = ledgerEntryRepository
                .findByOwnerUserIdAndPartyIdOrderByEntryDateDescIdDesc(userId, partyId, Pageable.ofSize(2000))
                .getContent();

        StringBuilder csv = new StringBuilder();
        csv.append("party_id,party_name,entry_date,direction,amount_minor,note,created_at\n");
        for (LedgerEntryEntity e : entries) {
            csv.append(partyId).append(',');
            csv.append(escape(party.getName())).append(',');
            csv.append(e.getEntryDate().format(ISO_DATE)).append(',');
            csv.append(escape(e.getDirection())).append(',');
            csv.append(e.getAmountMinor()).append(',');
            csv.append(escape(e.getNote())).append(',');
            csv.append(e.getCreatedAt()).append('\n');
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        String filename = "party-" + partyId + "-ledger.csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    private String escape(String value) {
        if (value == null) {
            return "\"\"";
        }
        String v = value.replace("\"", "\"\"");
        return "\"" + v + "\"";
    }
}
