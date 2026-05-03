package com.example.coreapp.audit.service;

import com.example.coreapp.audit.dto.AuditLogResponse;
import com.example.coreapp.audit.entity.AuditLogEntity;
import com.example.coreapp.audit.repository.AuditLogRepository;
import com.example.coreapp.common.web.PagedResponse;
import com.example.coreapp.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, CurrentUser currentUser, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void log(String action, String entityType, Long entityId, Object details) {
        long userId = currentUser.userId();

        AuditLogEntity e = new AuditLogEntity();
        e.setOwnerUserId(userId);
        e.setActorUserId(userId);
        e.setCorrelationId(MDC.get("correlationId"));
        e.setAction(action);
        e.setEntityType(entityType);
        e.setEntityId(entityId);
        e.setDetailsJson(toJson(details));
        e.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        auditLogRepository.save(e);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> list(int page, int size) {
        long userId = currentUser.userId();
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                clampSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<AuditLogEntity> p = auditLogRepository.findByOwnerUserIdOrderByCreatedAtDescIdDesc(userId, pageable);

        return new PagedResponse<>(
                p.getContent().stream().map(this::toResponse).toList(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.getNumber(),
                p.getSize(),
                p.hasNext()
        );
    }

    private AuditLogResponse toResponse(AuditLogEntity e) {
        return new AuditLogResponse(
                e.getId(),
                e.getActorUserId(),
                e.getCorrelationId(),
                e.getAction(),
                e.getEntityType(),
                e.getEntityId(),
                e.getDetailsJson(),
                e.getCreatedAt()
        );
    }

    private String toJson(Object details) {
        if (details == null) {
            return null;
        }
        if (details instanceof String s) {
            String trimmed = s.trim();
            return trimmed.isBlank() ? null : trimmed;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            return String.valueOf(details);
        }
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 200);
    }
}
