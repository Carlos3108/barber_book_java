package org.azdev.barber_book.services;

import org.azdev.barber_book.models.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

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
        org.azdev.barber_book.models.User appUser = buildAppUser("owner@test.com");

        String token = jwtService.generateToken(appUser);

        assertThat(jwtService.extractUsername(token)).isEqualTo("owner@test.com");
        assertThat(jwtService.extractTenantId(token)).isEqualTo(appUser.getTenant().getId().toString());
        assertThat(jwtService.isTokenValid(token, User.withUsername("owner@test.com").password("x").roles("ADMIN").build()))
                .isTrue();
    }

    @Test
    void isTokenValidReturnsFalseForDifferentUser() {
        org.azdev.barber_book.models.User appUser = buildAppUser("owner@test.com");
        String token = jwtService.generateToken(appUser);

        boolean valid = jwtService.isTokenValid(token, User.withUsername("other@test.com").password("x").roles("ADMIN").build());

        assertThat(valid).isFalse();
    }

    @Test
    void isTokenValidReturnsFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1L);
        org.azdev.barber_book.models.User appUser = buildAppUser("owner@test.com");
        String token = jwtService.generateToken(appUser);

        assertThatThrownBy(() -> jwtService.isTokenValid(
                token,
                User.withUsername("owner@test.com").password("x").roles("ADMIN").build()
        )).isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    private org.azdev.barber_book.models.User buildAppUser(String email) {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());

        org.azdev.barber_book.models.User user = new org.azdev.barber_book.models.User();
        user.setEmail(email);
        user.setPassword("pwd");
        user.setTenant(tenant);
        return user;
    }
}



