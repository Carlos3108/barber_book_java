package org.azdev.barber_book.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {
    public AuthenticatedUserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto atual.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticatedUserPrincipal) {
            return (AuthenticatedUserPrincipal) principal;
        }

        throw new IllegalStateException("O usuário autenticado não é do tipo esperado.");
    }

    public UUID getCurrentTenantId() {
        return getCurrentUser().tenantId();
    }

}
