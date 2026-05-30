package com.example.coreapp.audit.dto;

import java.time.OffsetDateTime;

public record AuditLogResponse(
        long id,
        long actorUserId,
        String correlationId,
        String action,
        String entityType,
        Long entityId,
        String detailsJson,
        OffsetDateTime createdAt
) {}
