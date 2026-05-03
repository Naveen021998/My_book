package com.example.coreapp.party.dto;

import java.time.OffsetDateTime;

public record PartyResponse(
        long id,
        String name,
        String phone,
        String type,
        long balanceMinor,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

