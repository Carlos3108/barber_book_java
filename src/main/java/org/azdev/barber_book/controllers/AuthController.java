package org.azdev.barber_book.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.AuthenticationRequest;
import org.azdev.barber_book.dtos.AuthenticationResponse;
import org.azdev.barber_book.dtos.RegisterRequest;
import org.azdev.barber_book.dtos.RegistrationResponse;
import org.azdev.barber_book.dtos.ResendVerificationRequest;
import org.azdev.barber_book.dtos.VerifyEmailRequest;
import org.azdev.barber_book.services.EmailVerificationService;
import org.azdev.barber_book.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints para registro e login de usuários")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    @Operation(summary = "Registra um novo usuário e seu estabelecimento (tenant)")
    public ResponseEntity<RegistrationResponse> register (
            @Valid @RequestBody RegisterRequest request){
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica um usuário e retorna um token JWT")
    public ResponseEntity<AuthenticationResponse> authenticate (
            @Valid @RequestBody AuthenticationRequest request){
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Confirma o e-mail com o código recebido e retorna um token JWT")
    public ResponseEntity<AuthenticationResponse> verifyEmail (
            @Valid @RequestBody VerifyEmailRequest request){
        return ResponseEntity.ok(emailVerificationService.verify(request.email(), request.code()));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Reenvia o código de verificação de e-mail")
    public ResponseEntity<Void> resendVerification (
            @Valid @RequestBody ResendVerificationRequest request){
        emailVerificationService.resend(request.email());
        return ResponseEntity.noContent().build();
    }
}