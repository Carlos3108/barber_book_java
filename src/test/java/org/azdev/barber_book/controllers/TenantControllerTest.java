package org.azdev.barber_book.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.azdev.barber_book.dtos.TenantResponse;
import org.azdev.barber_book.exception.GlobalExceptionHandler;
import org.azdev.barber_book.services.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TenantControllerTest {

    @Mock
    private TenantService tenantService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TenantController(tenantService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private record TenantPayload(String name, LocalTime openingTime, LocalTime closingTime, String timezone, Integer slotInterval) {
    }

    @Test
    void updateTenantSettingsReturnsNoContent() throws Exception {
        doNothing().when(tenantService).updateMyBarbershop(any());

        mockMvc.perform(put("/api/v1/settings/my-barbershop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TenantPayload(
                                "Shop", LocalTime.of(9, 0), LocalTime.of(18, 0), "America/Sao_Paulo", 30))))
                .andExpect(status().isNoContent());

        verify(tenantService).updateMyBarbershop(any());
    }

    @Test
    void getTenantSettingsReturnsSettings() throws Exception {
        when(tenantService.getMyBarbershopSettings()).thenReturn(
                new TenantResponse("Shop", LocalTime.of(9, 0), LocalTime.of(18, 0), "America/Sao_Paulo", 30));

        mockMvc.perform(get("/api/v1/settings/my-barbershop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Shop"));
    }
}
