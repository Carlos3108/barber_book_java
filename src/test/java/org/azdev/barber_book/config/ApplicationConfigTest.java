package org.azdev.barber_book.config;

import org.azdev.barber_book.models.User;
import org.azdev.barber_book.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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
    @Mock
    private AuthenticationConfiguration authenticationConfiguration;
    @Mock
    private AuthenticationManager authenticationManager;

    @Test
    void userDetailsServiceLoadsUserByEmail() {
        ApplicationConfig config = new ApplicationConfig(userRepository);
        User user = new User();
        user.setEmail("owner@test.com");

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user));
        UserDetailsService service = config.userDetailsService();

        assertThat(service.loadUserByUsername("owner@test.com")).isEqualTo(user);
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
    void authenticationProviderAndPasswordEncoderAreCreated() {
        ApplicationConfig config = new ApplicationConfig(userRepository);

        AuthenticationProvider provider = config.authenticationProvider();
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(provider).isNotNull();
        assertThat(encoder.matches("secret", encoder.encode("secret"))).isTrue();
    }

    @Test
    void authenticationManagerComesFromConfiguration() throws Exception {
        ApplicationConfig config = new ApplicationConfig(userRepository);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        AuthenticationManager manager = config.authenticationManager(authenticationConfiguration);

        assertThat(manager).isSameAs(authenticationManager);
    }
}

