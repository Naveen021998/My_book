package com.example.userservice.common.web;

import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class HealthController {

    @GetMapping("/health")
    public ServiceHealthResponse health() {
        return new ServiceHealthResponse(
                "USER_SERVICE_READY",
                "User service is available.",
                OffsetDateTime.now()
        );
    }
}

