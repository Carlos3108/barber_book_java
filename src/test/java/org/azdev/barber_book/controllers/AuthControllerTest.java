package org.azdev.barber_book.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.azdev.barber_book.dtos.AuthenticationResponse;
import org.azdev.barber_book.dtos.RegistrationResponse;
import org.azdev.barber_book.services.EmailVerificationService;
import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.exception.GlobalExceptionHandler;
import org.azdev.barber_book.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private EmailVerificationService emailVerificationService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, emailVerificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerRequiresEmailVerification() throws Exception {
        when(authService.register(any())).thenReturn(new RegistrationResponse("mail@test.com", true));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterPayload("shop", "owner", "mail@test.com", "12345678", true, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationRequired").value(true));
    }

    @Test
    void loginReturnsToken() throws Exception {
        when(authService.authenticate(any())).thenReturn(new AuthenticationResponse("login-token", "shop", "shop-slug"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("mail@test.com", "1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("login-token"));
    }

    @Test
    void verifyEmailReturnsToken() throws Exception {
        when(emailVerificationService.verify("mail@test.com", "123456"))
                .thenReturn(new AuthenticationResponse("verified-token", "shop", "shop-slug"));

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mail@test.com\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("verified-token"));
    }

    @Test
    void verifyEmailRejectsMalformedCode() throws Exception {
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mail@test.com\",\"code\":\"12ab\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendVerificationReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mail@test.com\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void loginWithUnverifiedEmailReturnsForbiddenWithCode() throws Exception {
        when(authService.authenticate(any()))
                .thenThrow(new org.azdev.barber_book.exception.EmailNotVerifiedException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("mail@test.com", "1234"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void loginWithInvalidCredentialsReturnsBadRequest() throws Exception {
        when(authService.authenticate(any()))
                .thenThrow(new BadRequestException("Credenciais inválidas."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("mail@test.com", "wrong-pass"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas."));
    }

    private record RegisterPayload(String shopName, String ownerName, String email, String password, boolean adultConfirmed, boolean termsAccepted) {
    }

    private record LoginPayload(String email, String password) {
    }
}

