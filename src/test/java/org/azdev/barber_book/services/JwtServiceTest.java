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

    private AuthenticatedUserPrincipal buildPrincipal(String email, String planStatus) {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setPlanStatus(planStatus);
        tenant.setName("Barbearia Central");

        return new AuthenticatedUserPrincipal(UUID.randomUUID(), email, "pwd", tenant.getId(), tenant.getPlanStatus());
    }
}



