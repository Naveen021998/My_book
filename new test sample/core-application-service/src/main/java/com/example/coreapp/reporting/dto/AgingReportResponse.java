package com.example.coreapp.reporting.dto;

public record AgingReportResponse(
        long receivable0To7,
        long receivable8To30,
        long receivable31To60,
        long receivable61To90,
        long receivable91Plus,
        long payable0To7,
        long payable8To30,
        long payable31To60,
        long payable61To90,
        long payable91Plus
) {}
