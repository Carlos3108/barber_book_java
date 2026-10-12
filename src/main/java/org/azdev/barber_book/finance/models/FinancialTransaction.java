package org.azdev.barber_book.finance.models;

import jakarta.persistence.*;
import lombok.*;
import org.azdev.barber_book.finance.enums.TransactionCategory;
import org.azdev.barber_book.finance.enums.TransactionType;
import org.azdev.barber_book.models.BaseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "financial_transactions")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialTransaction extends BaseEntity {

    @Column(nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private OffsetDateTime transactionDate;

    @Column(length = 255)
    private String description;

    @Column(name = "appointment_id")
    private UUID appointmentId;

    @Column(name = "professional_id")
    private UUID professionalId;

}
