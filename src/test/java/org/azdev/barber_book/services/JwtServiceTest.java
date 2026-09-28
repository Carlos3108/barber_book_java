package org.azdev.barber_book.services;

import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.security.AuthenticatedUserPrincipal;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "MqepRU4K6FiD58UbUQv89AScKd0d+OgkUlgcfX0Ts1n1SAoohYlIv5C5oBTf56ndDToXbA5exfZt5U9WHYofYA==";

    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 3_600_000L);
    }

    @Test
    void generateTokenAllowsClaimExtractionAndValidation() {
        AuthenticatedUserPrincipal appUser = buildPrincipal("owner@test.com", "TRIAL");

        String token = jwtService.generateToken(appUser);

        assertThat(jwtService.extractUsername(token)).isEqualTo("owner@test.com");
        assertThat(jwtService.extractTenantId(token)).isEqualTo(appUser.tenantId().toString());
        assertThat(jwtService.isTokenValid(token, buildPrincipal("owner@test.com", "TRIAL")))
                .isTrue();
    }

    @Test
    void isTokenValidReturnsFalseForDifferentUser() {
        AuthenticatedUserPrincipal appUser = buildPrincipal("owner@test.com", "TRIAL");
        String token = jwtService.generateToken(appUser);

        boolean valid = jwtService.isTokenValid(token, buildPrincipal("other@test.com", "TRIAL"));

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValidReturnsFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1L);
        AuthenticatedUserPrincipal appUser = buildPrincipal("owner@test.com", "TRIAL");
        String token = jwtService.generateToken(appUser);

        assertThatThrownBy(() -> jwtService.isTokenValid(
                token,
                buildPrincipal("owner@test.com", "TRIAL")
        )).isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void generateTokenWorksWithUrlSafeBase64Secret() {
        // Contains '-' and '_' so it fails standard Base64 but succeeds as Base64URL.
        ReflectionTestUtils.setField(jwtService, "secret", "a-b_c-d_e-f_g-h_i-j_k-l_m-n_o-p_q-r_s-t_u-v_w-x_y-z_012345");

        AuthenticatedUserPrincipal appUser = buildPrincipal("owner@test.com", "TRIAL");
        String token = jwtService.generateToken(appUser);

        assertThat(jwtService.extractUsername(token)).isEqualTo("owner@test.com");
    }

    @Test
    void generateTokenWorksWithRawUtf8FallbackSecret() {
        // Not valid Base64 (contains spaces and invalid padding chars), falls back to raw UTF-8 bytes.
        ReflectionTestUtils.setField(jwtService, "secret", "this is a plain raw secret string!! with spaces 123");

        AuthenticatedUserPrincipal appUser = buildPrincipal("owner@test.com", "TRIAL");
        String token = jwtService.generateToken(appUser);

        assertThat(jwtService.extractUsername(token)).isEqualTo("owner@test.com");
    }

    @Test
    void generateTokenThrowsWhenSecretIsBlank() {
        ReflectionTestUtils.setField(jwtService, "secret", "  ");

        assertThatThrownBy(() -> jwtService.generateToken(buildPrincipal("owner@test.com", "TRIAL")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generateTokenThrowsWhenSecretIsKnownWeakValue() {
        ReflectionTestUtils.setField(jwtService, "secret", "change_me");

        assertThatThrownBy(() -> jwtService.generateToken(buildPrincipal("owner@test.com", "TRIAL")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generateTokenThrowsWhenSecretIsTooShort() {
        ReflectionTestUtils.setField(jwtService, "secret", "short-secret");

        assertThatThrownBy(() -> jwtService.generateToken(buildPrincipal("owner@test.com", "TRIAL")))
                .isInstanceOf(IllegalStateException.class);
    }

    private AuthenticatedUserPrincipal buildPrincipal(String email, String planStatus) {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setPlanStatus(planStatus);
        tenant.setName("Barbearia Central");

        return new AuthenticatedUserPrincipal(UUID.randomUUID(), email, "pwd", tenant.getId(), tenant.getPlanStatus());
    }
}



