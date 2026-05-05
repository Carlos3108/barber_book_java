package org.azdev.barber_book.security;

import org.azdev.barber_book.models.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record AuthenticatedUserPrincipal(
        UUID id,
        String email,
        String password,
        UUID tenantId,
        String planStatus
) implements UserDetails {

    public static AuthenticatedUserPrincipal from(User user) {
        return new AuthenticatedUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getTenant().getId(),
                user.getTenant().getPlanStatus()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"SUSPENDED".equals(planStatus);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}