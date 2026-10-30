package org.azdev.barber_book.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "terms_accepted_at")
    private OffsetDateTime termsAcceptedAt;

    @Column(name = "legal_version", length = 20)
    private String legalVersion;

    @Column(name = "adult_confirmed_at")
    private OffsetDateTime adultConfirmedAt;

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    @Column(name = "email_verification_code_hash")
    private String emailVerificationCodeHash;

    @Column(name = "email_verification_expires_at")
    private OffsetDateTime emailVerificationExpiresAt;

    @Column(name = "email_verification_sent_at")
    private OffsetDateTime emailVerificationSentAt;

    @Column(name = "email_verification_attempts", nullable = false)
    private int emailVerificationAttempts;

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }
}
