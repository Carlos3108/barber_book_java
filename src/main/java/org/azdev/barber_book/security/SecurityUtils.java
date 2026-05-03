package org.azdev.barber_book.security;

import org.azdev.barber_book.models.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto atual.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            return (User) principal;
        }

        throw new IllegalStateException("O usuário atenticado não é do tipo esperado.");
    }

    public UUID getCurrentTenantId() {
        return getCurrentUser().getTenant().getId();
    }

}
