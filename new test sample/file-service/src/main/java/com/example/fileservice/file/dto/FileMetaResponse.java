package com.example.fileservice.file.dto;

import java.time.OffsetDateTime;

public record FileMetaResponse(
        long id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        OffsetDateTime createdAt,
        String downloadPath
) {
}

