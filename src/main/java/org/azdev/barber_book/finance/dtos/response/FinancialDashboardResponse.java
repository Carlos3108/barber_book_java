package org.azdev.barber_book.finance.dtos.response;

import java.math.BigDecimal;

public record FinancialDashboardResponse(
        BigDecimal totalRevenue,
        BigDecimal totalCommissions,
        BigDecimal fixedExpenses,
        BigDecimal netProfit)
{}
