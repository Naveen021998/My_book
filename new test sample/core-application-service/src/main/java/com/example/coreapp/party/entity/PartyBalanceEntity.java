package com.example.coreapp.party.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "party_balances")
public class PartyBalanceEntity {

    @Id
    @Column(name = "party_id", nullable = false)
    private Long partyId;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "balance_minor", nullable = false)
    private Long balanceMinor;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Long getPartyId() {
        return partyId;
    }

    public void setPartyId(Long partyId) {
        this.partyId = partyId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Long getBalanceMinor() {
        return balanceMinor;
    }

    public void setBalanceMinor(Long balanceMinor) {
        this.balanceMinor = balanceMinor;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

