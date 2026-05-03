package com.example.coreapp.ledger.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateLedgerEntryRequest(
        @NotBlank @Pattern(regexp = "CREDIT|DEBIT", message = "direction must be CREDIT or DEBIT") String direction,
        @NotNull @Min(1) Long amountMinor,
        @Size(max = 500) String note,
        @NotNull LocalDate entryDate
) {
}

