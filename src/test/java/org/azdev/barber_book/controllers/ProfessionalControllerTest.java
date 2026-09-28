package org.azdev.barber_book.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.azdev.barber_book.dtos.ProfessionalResponse;
import org.azdev.barber_book.exception.GlobalExceptionHandler;
import org.azdev.barber_book.services.ProfessionalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;
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
class ProfessionalControllerTest {

    @Mock
    private ProfessionalService professionalService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProfessionalController(professionalService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private record ProfessionalPayload(String name, Set<UUID> serviceIds) {
    }

    @Test
    void createReturnsCreated() throws Exception {
        UUID id = UUID.randomUUID();
        when(professionalService.createProfessional(any())).thenReturn(
                new ProfessionalResponse(id, "João", true, Set.of()));

        mockMvc.perform(post("/api/v1/professionals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProfessionalPayload("João", Set.of()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("João"));
    }

    @Test
    void listReturnsProfessionals() throws Exception {
        when(professionalService.listMyProfessionals()).thenReturn(List.of(
                new ProfessionalResponse(UUID.randomUUID(), "Maria", true, Set.of())));

        mockMvc.perform(get("/api/v1/professionals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Maria"));
    }

    @Test
    void getByIdReturnsProfessional() throws Exception {
        UUID id = UUID.randomUUID();
        when(professionalService.getProfessionalById(id)).thenReturn(
                new ProfessionalResponse(id, "João", true, Set.of()));

        mockMvc.perform(get("/api/v1/professionals/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void updateReturnsUpdatedProfessional() throws Exception {
        UUID id = UUID.randomUUID();
        when(professionalService.updateProfessional(eq(id), any())).thenReturn(
                new ProfessionalResponse(id, "Novo Nome", true, Set.of()));

        mockMvc.perform(put("/api/v1/professionals/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProfessionalPayload("Novo Nome", Set.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novo Nome"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(professionalService).deleteProfessional(id);

        mockMvc.perform(delete("/api/v1/professionals/" + id))
                .andExpect(status().isNoContent());

        verify(professionalService).deleteProfessional(id);
    }
}
