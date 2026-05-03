package com.example.coreapp.party.mapper;

import com.example.coreapp.party.dto.PartyResponse;
import com.example.coreapp.party.entity.PartyBalanceEntity;
import com.example.coreapp.party.entity.PartyEntity;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PartyMapper {

    public PartyResponse toResponse(PartyEntity party, PartyBalanceEntity balance) {
        return new PartyResponse(
                party.getId(),
                party.getName(),
                party.getPhone(),
                party.getType(),
                balance == null ? 0L : balance.getBalanceMinor(),
                party.getCreatedAt(),
                party.getUpdatedAt()
        );
    }

    public PartyResponse toResponse(PartyEntity party, Map<Long, PartyBalanceEntity> balanceByPartyId) {
        return toResponse(party, balanceByPartyId.get(party.getId()));
    }
}

