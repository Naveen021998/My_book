package com.example.notificationservice.notification.repository;

import com.example.notificationservice.notification.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Page<NotificationEntity> findByOwnerUserIdOrderByCreatedAtDescIdDesc(Long ownerUserId, Pageable pageable);
}

