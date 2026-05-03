package com.example.coreapp.reporting.dto;

public record SummaryReportResponse(
        long totalReceivableMinor,
        long totalPayableMinor
) {
}

