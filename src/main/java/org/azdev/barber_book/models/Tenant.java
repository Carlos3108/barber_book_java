package org.azdev.barber_book.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@Table(name = "tenants")
@NoArgsConstructor
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false, length = 100)
    private String slug; // Gerado via SlugService (ex: cortes-do-ze-x92b)

    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;

    @Column(name = "trial_expires_at", nullable = false)
    private LocalDateTime trialExpiresAt;

    @Column(name = "plan_status", nullable = false)
    private String planStatus = "TRIAL"; // TRIAL, ACTIVE, SUSPENDED
}
