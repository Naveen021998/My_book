package com.example.coreapp.reminder.repository;

import com.example.coreapp.reminder.entity.ReminderEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<ReminderEntity, Long> {

    Page<ReminderEntity> findByOwnerUserIdAndPartyIdOrderByCreatedAtDescIdDesc(long ownerUserId, long partyId, Pageable pageable);

    Optional<ReminderEntity> findByOwnerUserIdAndId(long ownerUserId, long id);

    long deleteByOwnerUserIdAndPartyId(long ownerUserId, long partyId);
}
