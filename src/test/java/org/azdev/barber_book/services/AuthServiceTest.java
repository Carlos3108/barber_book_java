package org.azdev.barber_book.services;

import org.azdev.barber_book.dtos.AuthenticationRequest;
import org.azdev.barber_book.dtos.AuthenticationResponse;
import org.azdev.barber_book.dtos.RegisterRequest;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.User;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SlugService slugService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesTenantAndUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("Barber Shop", "Carlos", "carlos@test.com", "123456");
        Tenant savedTenant = new Tenant();
        savedTenant.setId(UUID.randomUUID());
        User savedUser = new User();
        savedUser.setEmail(request.email());
        savedUser.setTenant(savedTenant);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(slugService.generateSlug(request.shopName())).thenReturn("barber-shop-abcd");
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("jwt-token");

        AuthenticationResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User createdUser = userCaptor.getValue();
        assertThat(createdUser.getPassword()).isEqualTo("encoded-password");
        assertThat(createdUser.getTenant()).isEqualTo(savedTenant);
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Barber Shop", "Carlos", "carlos@test.com", "123456");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Este e-mail já está cadastrado");
    }

    @Test
    void authenticateReturnsTokenWhenCredentialsAreValid() {
        AuthenticationRequest request = new AuthenticationRequest("carlos@test.com", "123456");
        User user = new User();
        user.setEmail(request.email());

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthenticationResponse response = authService.authenticate(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    }

    @Test
    void authenticatePropagatesAuthenticationFailure() {
        AuthenticationRequest request = new AuthenticationRequest("carlos@test.com", "wrong");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid credentials");
    }
}

