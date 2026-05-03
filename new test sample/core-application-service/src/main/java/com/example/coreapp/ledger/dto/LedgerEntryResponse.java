package com.example.coreapp.ledger.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record LedgerEntryResponse(
        long id,
        long partyId,
        String direction,
        long amountMinor,
        String note,
        LocalDate entryDate,
        OffsetDateTime createdAt
) {
}

