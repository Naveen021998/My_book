package com.example.fileservice.common.web;

import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
public class HealthController {

    @GetMapping("/health")
    public ServiceHealthResponse health() {
        return new ServiceHealthResponse(
                "FILE_SERVICE_READY",
                "File service is available.",
                OffsetDateTime.now()
        );
    }
}

