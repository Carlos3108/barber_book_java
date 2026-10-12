package org.azdev.barber_book.finance.repositories;

import org.azdev.barber_book.finance.models.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, UUID> {

    @Query("""
        SELECT COALESCE(SUM(f.amount), 0) FROM FinancialTransaction f 
        WHERE f.tenantId = :tenantId 
        AND f.type = 'CREDIT' 
        AND f.transactionDate >= :startDate 
        AND f.transactionDate <= :endDate
    """)
    BigDecimal sumRevenueByTenantAndPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query("""
        SELECT COALESCE(SUM(f.amount), 0) FROM FinancialTransaction f 
        WHERE f.tenantId = :tenantId 
        AND f.professionalId = :professionalId
        AND f.category = 'COMMISSION_PAYMENT'
        AND f.type = 'DEBIT'
        AND f.transactionDate >= :startDate 
        AND f.transactionDate <= :endDate
    """)
    BigDecimal sumCommissionsByProfessionalAndPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("professionalId") UUID professionalId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query("""
        SELECT COALESCE(SUM(f.amount), 0) FROM FinancialTransaction f 
        WHERE f.tenantId = :tenantId 
        AND f.category = 'FIXED_EXPENSE'
        AND f.type = 'DEBIT'
        AND f.transactionDate >= :startDate 
        AND f.transactionDate <= :endDate
    """)
    BigDecimal sumFixedExpensesByTenantAndPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query("""
        SELECT COALESCE(SUM(f.amount), 0) FROM FinancialTransaction f 
        WHERE f.tenantId = :tenantId 
        AND f.category = 'COMMISSION_PAYMENT'
        AND f.type = 'DEBIT'
        AND f.transactionDate >= :startDate 
        AND f.transactionDate <= :endDate
    """)
    BigDecimal sumTotalCommissionsByTenantAndPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );
}
