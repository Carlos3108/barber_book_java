package org.azdev.barber_book.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Getter @Setter
@Table(name = "tenants")
@NoArgsConstructor
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false, length = 100)
    private String slug;

    @Column(name = "trial_expires_at", nullable = false)
    private OffsetDateTime trialExpiresAt;

    @Column(name = "plan_status", nullable = false)
    private String planStatus = "TRIAL"; // TRIAL, ACTIVE, SUSPENDED

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime = LocalTime.of(9, 0);

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime = LocalTime.of(18, 0);

    @Column(name = "timezone", nullable = false)
    private String timezone = "America/Sao_Paulo";
}
