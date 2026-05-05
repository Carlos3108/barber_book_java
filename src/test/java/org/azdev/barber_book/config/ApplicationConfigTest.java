package org.azdev.barber_book.config;

import org.azdev.barber_book.models.User;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.security.AuthenticatedUserPrincipal;
import org.azdev.barber_book.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void userDetailsServiceLoadsUserByEmail() {
        ApplicationConfig config = new ApplicationConfig(userRepository);
        User user = new User();
        user.setEmail("owner@test.com");
        Tenant tenant = new Tenant();
        tenant.setPlanStatus("ACTIVE");
        tenant.setName("Barbearia Central");
        user.setTenant(tenant);

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user));
        UserDetailsService service = config.userDetailsService();

        assertThat(service.loadUserByUsername("owner@test.com"))
                .isInstanceOf(AuthenticatedUserPrincipal.class)
            .extracting("username")
                .isEqualTo("owner@test.com");
    }

    @Test
    void userDetailsServiceThrowsWhenEmailNotFound() {
        ApplicationConfig config = new ApplicationConfig(userRepository);
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> config.userDetailsService().loadUserByUsername("missing@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Usuário não encontrado");
    }

    @Test
    void authenticationManagerAndPasswordEncoderAreCreated() {
        ApplicationConfig config = new ApplicationConfig(userRepository);
        Tenant tenant = new Tenant();
        tenant.setPlanStatus("ACTIVE");
        tenant.setName("Barbearia Central");
        User user = new User();
        user.setEmail("owner@test.com");
        user.setPassword(config.passwordEncoder().encode("secret"));
        user.setTenant(tenant);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user));

        AuthenticationManager manager = config.authenticationManager();
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(manager.authenticate(new UsernamePasswordAuthenticationToken("owner@test.com", "secret"))).isNotNull();
        assertThat(encoder.matches("secret", encoder.encode("secret"))).isTrue();
    }
}
