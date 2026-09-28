package org.azdev.barber_book.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    private final SecurityUtils securityUtils = new SecurityUtils();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserThrowsWhenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(securityUtils::getCurrentUser)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getCurrentUserThrowsWhenNotAuthenticated() {
        TestingAuthenticationToken token = new TestingAuthenticationToken("user", "pass");
        token.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThatThrownBy(securityUtils::getCurrentUser)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getCurrentUserThrowsWhenPrincipalIsWrongType() {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("not-a-principal", null);
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThatThrownBy(securityUtils::getCurrentUser)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getCurrentUserReturnsPrincipalWhenAuthenticated() {
        UUID tenantId = UUID.randomUUID();
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                UUID.randomUUID(), "user@test.com", "pw", tenantId, "ACTIVE");
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThat(securityUtils.getCurrentUser()).isEqualTo(principal);
    }

    @Test
    void getCurrentTenantIdReturnsTenantIdFromPrincipal() {
        UUID tenantId = UUID.randomUUID();
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                UUID.randomUUID(), "user@test.com", "pw", tenantId, "ACTIVE");
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);

        assertThat(securityUtils.getCurrentTenantId()).isEqualTo(tenantId);
    }
}
