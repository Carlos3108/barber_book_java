package org.azdev.barber_book.services;

import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.AuthenticationRequest;
import org.azdev.barber_book.dtos.AuthenticationResponse;
import org.azdev.barber_book.dtos.RegisterRequest;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.User;
import org.azdev.barber_book.security.AuthenticatedUserPrincipal;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.repositories.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final SlugService slugService;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request){
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado");
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.shopName());
        tenant.setSlug(slugService.generateSlug(request.shopName()));
        tenant.setOwnerEmail(request.email());
        tenant.setTrialExpiresAt(LocalDateTime.now().plusDays(30));
        tenant.setPlanStatus("TRIAL");
        tenant = tenantRepository.save(tenant);

        User user = new User();
        user.setName(request.ownerName());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setTenant(tenant);
        user = userRepository.save(user);

        var jwtToken = jwtService.generateToken(AuthenticatedUserPrincipal.from(user));

        return new AuthenticationResponse(jwtToken);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        var user = userRepository.findByEmail(request.email())
                .orElseThrow();

        var jwtToken = jwtService.generateToken(AuthenticatedUserPrincipal.from(user));

        return new AuthenticationResponse(jwtToken);
    }
}
