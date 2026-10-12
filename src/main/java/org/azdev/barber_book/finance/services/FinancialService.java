package org.azdev.barber_book.finance.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.azdev.barber_book.finance.dtos.response.FinancialDashboardResponse;
import org.azdev.barber_book.finance.enums.TransactionCategory;
import org.azdev.barber_book.finance.enums.TransactionType;
import org.azdev.barber_book.finance.models.FinancialTransaction;
import org.azdev.barber_book.finance.events.AppointmentCompletedEvent;
import org.azdev.barber_book.finance.repositories.FinancialTransactionRepository;
import org.azdev.barber_book.finance.strategy.CommissionStrategy;
import org.azdev.barber_book.finance.strategy.PercentageCommissionStrategy;
import org.azdev.barber_book.security.SecurityUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialService {

    private final FinancialTransactionRepository financialRepository;
    private final SecurityUtils securityUtils;

    @EventListener
    @Transactional
    public void handleAppointmentCompleted(AppointmentCompletedEvent event) {
        log.info("Processando financeiro do agendamento concluído: {}", event.appointmentId());

        OffsetDateTime now = OffsetDateTime.now();

        FinancialTransaction revenue = FinancialTransaction.builder()
                .tenantId(event.tenantId())
                .appointmentId(event.appointmentId())
                .professionalId(event.professionalId())
                .type(TransactionType.CREDIT)
                .category(TransactionCategory.SERVICE_REVENUE)
                .amount(event.servicePrice())
                .transactionDate(now)
                .description("Faturamento de serviço concluído")
                .build();

        financialRepository.save(revenue);

        CommissionStrategy strategy = new PercentageCommissionStrategy(new BigDecimal("50.00"));
        BigDecimal commissionAmount = strategy.calculateCommission(event.servicePrice());

        FinancialTransaction commission = FinancialTransaction.builder()
                .tenantId(event.tenantId())
                .appointmentId(event.appointmentId())
                .professionalId(event.professionalId())
                .type(TransactionType.DEBIT)
                .category(TransactionCategory.COMMISSION_PAYMENT)
                .amount(commissionAmount)
                .transactionDate(now)
                .description("Comissão provisionada para o profissional")
                .build();

        financialRepository.save(commission);

        log.info("Lançamentos gerados. Receita: R$ {}, Comissão a Pagar: R$ {}",
                event.servicePrice(), commissionAmount);
    }

    @Transactional(readOnly = true)
    public FinancialDashboardResponse getDashboardMetrics(OffsetDateTime startDate, OffsetDateTime endDate) {

        UUID tenantId = securityUtils.getCurrentTenantId();

        BigDecimal revenue = financialRepository.sumRevenueByTenantAndPeriod(tenantId, startDate, endDate);
        BigDecimal commissions = financialRepository.sumTotalCommissionsByTenantAndPeriod(tenantId, startDate, endDate);
        BigDecimal fixedExpenses = financialRepository.sumFixedExpensesByTenantAndPeriod(tenantId, startDate, endDate);

        BigDecimal totalDebits = commissions.add(fixedExpenses);
        BigDecimal netProfit = revenue.subtract(totalDebits);

        return new FinancialDashboardResponse(revenue, commissions, fixedExpenses, netProfit);
    }
}
