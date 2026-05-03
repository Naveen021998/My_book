package com.example.notificationservice.notification.mapper;

import com.example.notificationservice.notification.dto.NotificationResponse;
import com.example.notificationservice.notification.entity.NotificationEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(NotificationEntity entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getChannel(),
                entity.getDestination(),
                entity.getTitle(),
                entity.getBody(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}

