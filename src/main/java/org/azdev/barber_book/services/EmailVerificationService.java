package org.azdev.barber_book.services;

import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.AuthenticationResponse;
import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.models.User;
import org.azdev.barber_book.repositories.UserRepository;
import org.azdev.barber_book.security.AuthenticatedUserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    static final int CODE_VALIDITY_MINUTES = 15;
    static final int MAX_ATTEMPTS = 5;
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final String INVALID_CODE_MESSAGE = "Código inválido ou expirado.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailSender emailSender;
    private final Clock clock;

    private final SecureRandom random = new SecureRandom();

    /** Gera um novo código, grava apenas o hash e envia o e-mail. */
    public void issueCode(User user) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        String code = String.format("%06d", random.nextInt(1_000_000));

        user.setEmailVerificationCodeHash(passwordEncoder.encode(code));
        user.setEmailVerificationExpiresAt(now.plusMinutes(CODE_VALIDITY_MINUTES));
        user.setEmailVerificationSentAt(now);
        user.setEmailVerificationAttempts(0);
        userRepository.save(user);

        emailSender.sendVerificationCode(user.getEmail(), code, CODE_VALIDITY_MINUTES);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public AuthenticationResponse verify(String email, String code) {
        User user = userRepository.findByEmail(email)
                .filter(u -> !u.isEmailVerified())
                .orElseThrow(() -> new BadRequestException(INVALID_CODE_MESSAGE));

        OffsetDateTime now = OffsetDateTime.now(clock);
        if (user.getEmailVerificationCodeHash() == null
                || user.getEmailVerificationExpiresAt() == null
                || now.isAfter(user.getEmailVerificationExpiresAt())
                || user.getEmailVerificationAttempts() >= MAX_ATTEMPTS) {
            throw new BadRequestException(INVALID_CODE_MESSAGE);
        }

        if (!passwordEncoder.matches(code, user.getEmailVerificationCodeHash())) {
            user.setEmailVerificationAttempts(user.getEmailVerificationAttempts() + 1);
            userRepository.save(user);
            throw new BadRequestException(INVALID_CODE_MESSAGE);
        }

        user.setEmailVerifiedAt(now);
        user.setEmailVerificationCodeHash(null);
        user.setEmailVerificationExpiresAt(null);
        user.setEmailVerificationAttempts(0);
        userRepository.save(user);

        String token = jwtService.generateToken(AuthenticatedUserPrincipal.from(user));
        return new AuthenticationResponse(token, user.getTenant().getName(), user.getTenant().getSlug());
    }

    /** Resposta sempre igual, exista ou não o e-mail, para não revelar cadastros. */
    @Transactional
    public void resend(String email) {
        userRepository.findByEmail(email)
                .filter(u -> !u.isEmailVerified())
                .filter(u -> u.getEmailVerificationSentAt() == null
                        || OffsetDateTime.now(clock).isAfter(u.getEmailVerificationSentAt().plus(RESEND_COOLDOWN)))
                .ifPresent(this::issueCode);
    }
}
