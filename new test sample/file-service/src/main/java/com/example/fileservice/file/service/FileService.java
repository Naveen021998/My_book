package com.example.fileservice.file.service;

import com.example.fileservice.file.dto.FileMetaResponse;
import com.example.fileservice.file.entity.FileEntity;
import com.example.fileservice.file.repository.FileRepository;
import com.example.fileservice.common.exception.ResourceNotFoundException;
import com.example.fileservice.security.CurrentUser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileService {

    private final FileRepository fileRepository;
    private final CurrentUser currentUser;
    private final FileStorageProperties fileStorageProperties;

    public FileService(FileRepository fileRepository, CurrentUser currentUser, FileStorageProperties fileStorageProperties) {
        this.fileRepository = fileRepository;
        this.currentUser = currentUser;
        this.fileStorageProperties = fileStorageProperties;
    }

    @Transactional
    public FileMetaResponse upload(MultipartFile file) throws IOException {
        long userId = currentUser.userId();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }

        String originalName = Objects.requireNonNullElse(file.getOriginalFilename(), "upload.bin");
        String contentType = Objects.requireNonNullElse(file.getContentType(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
        long size = file.getSize();

        Path dir = Path.of(fileStorageProperties.getStorageDir()).toAbsolutePath().normalize();
        Files.createDirectories(dir);

        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String storageKey = UUID.randomUUID().toString();
        Path path = dir.resolve(storageKey + "_" + safeName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }

        FileEntity entity = new FileEntity();
        entity.setOwnerUserId(userId);
        entity.setOriginalFilename(originalName);
        entity.setContentType(contentType);
        entity.setSizeBytes(size);
        entity.setStoragePath(path.toString());

        try {
            FileEntity saved = fileRepository.save(entity);
            return toMeta(saved);
        } catch (RuntimeException exception) {
            // Best-effort cleanup: if DB save fails after file write, remove the orphaned file.
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public FileMetaResponse getMeta(long id) {
        long userId = currentUser.userId();
        FileEntity entity = fileRepository.findByIdAndOwnerUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found."));
        return toMeta(entity);
    }

    @Transactional(readOnly = true)
    public DownloadFile download(long id) {
        long userId = currentUser.userId();
        FileEntity entity = fileRepository.findByIdAndOwnerUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found."));
        Resource resource = new FileSystemResource(entity.getStoragePath());
        return new DownloadFile(entity, resource);
    }

    private FileMetaResponse toMeta(FileEntity entity) {
        return new FileMetaResponse(
                entity.getId(),
                entity.getOriginalFilename(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getCreatedAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : entity.getCreatedAt(),
                "/api/v1/files/" + entity.getId() + "/download"
        );
    }

    public record DownloadFile(FileEntity meta, Resource resource) {}
}
