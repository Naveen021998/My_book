package com.example.coreapp.ledger.mapper;

import com.example.coreapp.ledger.dto.LedgerEntryResponse;
import com.example.coreapp.ledger.entity.LedgerEntryEntity;
import org.springframework.stereotype.Component;

@Component
public class LedgerEntryMapper {

    public LedgerEntryResponse toResponse(LedgerEntryEntity entity) {
        return new LedgerEntryResponse(
                entity.getId(),
                entity.getPartyId(),
                entity.getDirection(),
                entity.getAmountMinor(),
                entity.getNote(),
                entity.getEntryDate(),
                entity.getCreatedAt()
        );
    }
}

