package com.example.fileservice.file.repository;

import com.example.fileservice.file.entity.FileEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

    Optional<FileEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}

