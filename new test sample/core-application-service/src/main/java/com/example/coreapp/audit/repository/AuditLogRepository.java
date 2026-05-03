package com.example.coreapp.audit.repository;

import com.example.coreapp.audit.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    Page<AuditLogEntity> findByOwnerUserIdOrderByCreatedAtDescIdDesc(long ownerUserId, Pageable pageable);
}
