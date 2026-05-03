package com.example.coreapp.reporting.service;

import com.example.coreapp.party.repository.PartyBalanceRepository;
import com.example.coreapp.reporting.dto.AgingReportResponse;
import com.example.coreapp.reporting.dto.SummaryReportResponse;
import com.example.coreapp.security.CurrentUser;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingService {

    private final PartyBalanceRepository partyBalanceRepository;
    private final CurrentUser currentUser;

    public ReportingService(PartyBalanceRepository partyBalanceRepository, CurrentUser currentUser) {
        this.partyBalanceRepository = partyBalanceRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public SummaryReportResponse summary() {
        long userId = currentUser.userId();
        Object[] row = partyBalanceRepository.summaryTotals(userId);

        long receivable = row == null || row.length < 2 || row[0] == null ? 0L : ((Number) row[0]).longValue();
        long payable = row == null || row.length < 2 || row[1] == null ? 0L : ((Number) row[1]).longValue();

        return new SummaryReportResponse(receivable, payable);
    }

    @Transactional(readOnly = true)
    public AgingReportResponse aging() {
        long userId = currentUser.userId();
        List<Object[]> rows = partyBalanceRepository.balancesWithLastActivity(userId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        long r0_7 = 0L;
        long r8_30 = 0L;
        long r31_60 = 0L;
        long r61_90 = 0L;
        long r91p = 0L;

        long p0_7 = 0L;
        long p8_30 = 0L;
        long p31_60 = 0L;
        long p61_90 = 0L;
        long p91p = 0L;

        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }

            long balanceMinor = ((Number) row[0]).longValue();
            LocalDate last = toLocalDate(row[1]);
            long ageDays = last == null ? 0L : Math.max(0L, ChronoUnit.DAYS.between(last, today));

            boolean receivable = balanceMinor > 0;
            boolean payable = balanceMinor < 0;
            if (!receivable && !payable) {
                continue;
            }

            long amount = receivable ? balanceMinor : -balanceMinor;

            if (ageDays <= 7) {
                if (receivable) r0_7 += amount; else p0_7 += amount;
            } else if (ageDays <= 30) {
                if (receivable) r8_30 += amount; else p8_30 += amount;
            } else if (ageDays <= 60) {
                if (receivable) r31_60 += amount; else p31_60 += amount;
            } else if (ageDays <= 90) {
                if (receivable) r61_90 += amount; else p61_90 += amount;
            } else {
                if (receivable) r91p += amount; else p91p += amount;
            }
        }

        return new AgingReportResponse(
                r0_7, r8_30, r31_60, r61_90, r91p,
                p0_7, p8_30, p31_60, p61_90, p91p
        );
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate d) {
            return d;
        }
        if (value instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (value instanceof java.util.Date d) {
            return new java.sql.Date(d.getTime()).toLocalDate();
        }
        return null;
    }
}
