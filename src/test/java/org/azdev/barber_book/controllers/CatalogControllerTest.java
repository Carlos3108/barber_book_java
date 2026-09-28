package org.azdev.barber_book.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.azdev.barber_book.dtos.CatalogResponse;
import org.azdev.barber_book.exception.GlobalExceptionHandler;
import org.azdev.barber_book.services.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    @Mock
    private CatalogService catalogService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CatalogController(catalogService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private record CatalogPayload(String name, BigDecimal price, Integer durationMinutes) {
    }

    @Test
    void createReturnsCreated() throws Exception {
        UUID id = UUID.randomUUID();
        when(catalogService.createService(any())).thenReturn(
                new CatalogResponse(id, "Corte", new BigDecimal("30.00"), 30, true));

        mockMvc.perform(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CatalogPayload("Corte", new BigDecimal("30.00"), 30))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Corte"));
    }

    @Test
    void listReturnsServices() throws Exception {
        when(catalogService.listMyServices()).thenReturn(List.of(
                new CatalogResponse(UUID.randomUUID(), "Barba", new BigDecimal("20.00"), 20, true)));

        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Barba"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(catalogService).deleteService(id);

        mockMvc.perform(delete("/api/v1/services/" + id))
                .andExpect(status().isNoContent());

        verify(catalogService).deleteService(id);
    }

    @Test
    void getByIdReturnsService() throws Exception {
        UUID id = UUID.randomUUID();
        when(catalogService.getMyServiceById(id)).thenReturn(
                new CatalogResponse(id, "Corte", new BigDecimal("30.00"), 30, true));

        mockMvc.perform(get("/api/v1/services/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void updateReturnsUpdatedService() throws Exception {
        UUID id = UUID.randomUUID();
        when(catalogService.updateService(eq(id), any())).thenReturn(
                new CatalogResponse(id, "Corte Premium", new BigDecimal("40.00"), 40, true));

        mockMvc.perform(put("/api/v1/services/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CatalogPayload("Corte Premium", new BigDecimal("40.00"), 40))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Corte Premium"));
    }
}
