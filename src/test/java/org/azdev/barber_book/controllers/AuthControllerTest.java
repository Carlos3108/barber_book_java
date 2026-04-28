package org.azdev.barber_book.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.azdev.barber_book.dtos.AuthenticationResponse;
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

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService)).build();
    }

    @Test
    void registerReturnsToken() throws Exception {
        when(authService.register(any())).thenReturn(new AuthenticationResponse("register-token"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterPayload("shop", "owner", "mail@test.com", "1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("register-token"));
    }

    @Test
    void loginReturnsToken() throws Exception {
        when(authService.authenticate(any())).thenReturn(new AuthenticationResponse("login-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginPayload("mail@test.com", "1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("login-token"));
    }

    private record RegisterPayload(String shopName, String ownerName, String email, String password) {
    }

    private record LoginPayload(String email, String password) {
    }
}

