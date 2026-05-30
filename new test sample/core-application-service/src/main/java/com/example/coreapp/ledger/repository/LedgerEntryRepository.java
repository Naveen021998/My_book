package com.example.coreapp.ledger.repository;

import com.example.coreapp.ledger.entity.LedgerEntryEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, Long> {

    Page<LedgerEntryEntity> findByOwnerUserIdAndPartyIdOrderByEntryDateDescIdDesc(Long ownerUserId, Long partyId, Pageable pageable);

    Optional<LedgerEntryEntity> findByOwnerUserIdAndIdempotencyKey(Long ownerUserId, String idempotencyKey);

    long deleteByOwnerUserIdAndPartyId(Long ownerUserId, Long partyId);
}
