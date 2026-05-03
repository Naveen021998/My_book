package com.example.coreapp.reporting.controller;

import com.example.coreapp.reporting.dto.AgingReportResponse;
import com.example.coreapp.reporting.dto.SummaryReportResponse;
import com.example.coreapp.reporting.service.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryReportResponse> summary() {
        return ResponseEntity.ok(reportingService.summary());
    }

    @GetMapping("/aging")
    public ResponseEntity<AgingReportResponse> aging() {
        return ResponseEntity.ok(reportingService.aging());
    }
}
