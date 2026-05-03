package com.example.notificationservice.notification.controller;

import com.example.notificationservice.common.web.PagedResponse;
import com.example.notificationservice.notification.dto.NotificationResponse;
import com.example.notificationservice.notification.dto.SendNotificationRequest;
import com.example.notificationservice.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody SendNotificationRequest request) {
        NotificationResponse response = notificationService.send(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .replacePath("/api/v1/notifications")
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED).location(location).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(notificationService.getNotifications(page, size));
    }
}

