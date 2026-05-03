package com.example.notificationservice.notification.service;

import com.example.notificationservice.common.web.PagedResponse;
import com.example.notificationservice.notification.dto.NotificationResponse;
import com.example.notificationservice.notification.dto.SendNotificationRequest;
import com.example.notificationservice.notification.entity.NotificationEntity;
import com.example.notificationservice.notification.mapper.NotificationMapper;
import com.example.notificationservice.notification.repository.NotificationRepository;
import com.example.notificationservice.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final CurrentUser currentUser;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper,
            CurrentUser currentUser
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
        this.currentUser = currentUser;
    }

    @Transactional
    public NotificationResponse send(SendNotificationRequest request) {
        long userId = currentUser.userId();

        // For now: persist as QUEUED. A real implementation would enqueue to a broker and process asynchronously.
        NotificationEntity entity = new NotificationEntity();
        entity.setOwnerUserId(userId);
        entity.setChannel(request.channel().trim().toUpperCase());
        entity.setDestination(request.destination().trim());
        entity.setTitle(request.title() == null || request.title().isBlank() ? null : request.title().trim());
        entity.setBody(request.body().trim());
        entity.setStatus("QUEUED");

        return notificationMapper.toResponse(notificationRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getNotifications(int page, int size) {
        long userId = currentUser.userId();
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                clampSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<NotificationResponse> result = notificationRepository
                .findByOwnerUserIdOrderByCreatedAtDescIdDesc(userId, pageable)
                .map(notificationMapper::toResponse);

        return PagedResponse.from(result);
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }
}

