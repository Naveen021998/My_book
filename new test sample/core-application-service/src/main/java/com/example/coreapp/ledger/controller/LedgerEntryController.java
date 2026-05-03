package com.example.coreapp.ledger.controller;

import com.example.coreapp.common.web.PagedResponse;
import com.example.coreapp.ledger.dto.CreateLedgerEntryRequest;
import com.example.coreapp.ledger.dto.LedgerEntryResponse;
import com.example.coreapp.ledger.service.LedgerService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/parties/{partyId}/entries")
public class LedgerEntryController {

    private final LedgerService ledgerService;

    public LedgerEntryController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping
    public ResponseEntity<LedgerEntryResponse> create(
            @PathVariable long partyId,
            @Valid @RequestBody CreateLedgerEntryRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        LedgerEntryResponse response = ledgerService.createEntry(partyId, request, idempotencyKey);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<LedgerEntryResponse>> list(
            @PathVariable long partyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(ledgerService.getEntries(partyId, page, size));
    }
}

