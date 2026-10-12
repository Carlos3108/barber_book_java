package org.azdev.barber_book.finance.controllers;

import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.finance.dtos.response.FinancialDashboardResponse;
import org.azdev.barber_book.finance.services.FinancialService;
import org.azdev.barber_book.security.SecurityUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;


@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinancialController {

    private final FinancialService financialService;
    private final SecurityUtils securityUtils;

    @GetMapping("/dashboard")
    public ResponseEntity<FinancialDashboardResponse> getDashboard(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
                                                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {

        FinancialDashboardResponse response = financialService.getDashboardMetrics(startDate, endDate);
        return ResponseEntity.ok(response);

    }
}
