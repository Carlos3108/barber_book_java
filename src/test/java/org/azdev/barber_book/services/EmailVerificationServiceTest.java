package org.azdev.barber_book.services;

import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.User;
import org.azdev.barber_book.repositories.UserRepository;
import org.azdev.barber_book.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-30T12:00:00Z"), ZoneOffset.UTC);
    private UserRepository userRepository;
    private JwtService jwtService;
    private EmailSender emailSender;
    private EmailVerificationService service;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        jwtService = mock(JwtService.class);
        emailSender = mock(EmailSender.class);
        service = new EmailVerificationService(userRepository, new BCryptPasswordEncoder(), jwtService, emailSender, clock);

        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("Barbearia");
        tenant.setSlug("barbearia");
        tenant.setPlanStatus("TRIAL");
        user = new User();
        user.setEmail("carlos@test.com");
        user.setTenant(tenant);
        when(userRepository.findByEmail("carlos@test.com")).thenReturn(Optional.of(user));
    }

    private String issueAndCaptureCode() {
        service.issueCode(user);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendVerificationCode(anyString(), code.capture(), anyInt());
        return code.getValue();
    }

    @Test
    void issueCodeStoresOnlyHashAndSendsSixDigitCode() {
        String code = issueAndCaptureCode();

        assertThat(code).matches("\\d{6}");
        assertThat(user.getEmailVerificationCodeHash()).isNotEqualTo(code).isNotBlank();
        assertThat(user.getEmailVerificationExpiresAt()).isAfter(user.getEmailVerificationSentAt());
    }

    @Test
    void verifyWithCorrectCodeMarksVerifiedAndReturnsToken() {
        String code = issueAndCaptureCode();
        when(jwtService.generateToken(any(AuthenticatedUserPrincipal.class))).thenReturn("jwt-token");

        var response = service.verify("carlos@test.com", code);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerificationCodeHash()).isNull();
    }

    @Test
    void verifyWithWrongCodeCountsAttemptAndFails() {
        String code = issueAndCaptureCode();
        String wrong = code.equals("000000") ? "111111" : "000000";

        assertThatThrownBy(() -> service.verify("carlos@test.com", wrong))
                .isInstanceOf(BadRequestException.class);
        assertThat(user.getEmailVerificationAttempts()).isEqualTo(1);
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void verifyBlocksAfterMaxAttemptsEvenWithCorrectCode() {
        String code = issueAndCaptureCode();
        user.setEmailVerificationAttempts(EmailVerificationService.MAX_ATTEMPTS);

        assertThatThrownBy(() -> service.verify("carlos@test.com", code))
                .isInstanceOf(BadRequestException.class);
        assertThat(user.isEmailVerified()).isFalse();
    }

    @Test
    void verifyRejectsExpiredCode() {
        String code = issueAndCaptureCode();
        user.setEmailVerificationExpiresAt(user.getEmailVerificationExpiresAt().minusHours(1));

        assertThatThrownBy(() -> service.verify("carlos@test.com", code))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void verifyRejectsUnknownEmailWithSameMessage() {
        when(userRepository.findByEmail("x@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("x@test.com", "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(EmailVerificationService.INVALID_CODE_MESSAGE);
    }

    @Test
    void resendRespectsCooldown() {
        service.issueCode(user);

        service.resend("carlos@test.com");

        verify(emailSender, org.mockito.Mockito.times(1)).sendVerificationCode(anyString(), anyString(), anyInt());
    }

    @Test
    void resendSkipsAlreadyVerifiedUser() {
        user.setEmailVerifiedAt(java.time.OffsetDateTime.now(clock));

        service.resend("carlos@test.com");

        verify(emailSender, never()).sendVerificationCode(anyString(), anyString(), anyInt());
    }
}
