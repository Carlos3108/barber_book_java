package org.azdev.barber_book.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.azdev.barber_book.dtos.CatalogResponse;
import org.azdev.barber_book.dtos.ProfessionalResponse;
import org.azdev.barber_book.exception.GlobalExceptionHandler;
import org.azdev.barber_book.exception.NotFoundException;
import org.azdev.barber_book.services.AppointmentService;
import org.azdev.barber_book.services.CatalogService;
import org.azdev.barber_book.services.ProfessionalService;
import org.azdev.barber_book.services.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicControllerTest {

    @Mock
    private TenantService tenantService;
    @Mock
    private CatalogService catalogService;
    @Mock
    private ProfessionalService professionalService;
    @Mock
    private AppointmentService appointmentService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PublicController(tenantService, catalogService, professionalService, appointmentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getBarbershopInfoReturnsName() throws Exception {
        when(tenantService.getPublicInfoBySlug("shop")).thenReturn(Map.of("name", "Shop"));

        mockMvc.perform(get("/api/v1/public/barbershop/shop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Shop"));
    }

    @Test
    void getBarbershopServicesReturnsList() throws Exception {
        when(catalogService.getPublicServicesBySlug("shop")).thenReturn(List.of(
                new CatalogResponse(UUID.randomUUID(), "Corte", new BigDecimal("30.00"), 30, true)));

        mockMvc.perform(get("/api/v1/public/barbershop/shop/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Corte"));
    }

    @Test
    void getBarbershopProfessionalsReturnsList() throws Exception {
        when(professionalService.getPublicProfessionalsBySlug("shop")).thenReturn(List.of(
                new ProfessionalResponse(UUID.randomUUID(), "João", true, Set.of())));

        mockMvc.perform(get("/api/v1/public/barbershop/shop/professionals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("João"));
    }

    @Test
    void getProfessionalSlotsReturnsAvailableSlots() throws Exception {
        UUID professionalId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(appointmentService.getAvailableSlots(any(), any(), any())).thenReturn(List.of("09:00", "09:30"));

        mockMvc.perform(get("/api/v1/public/professionals/" + professionalId + "/slots")
                        .param("date", LocalDate.now().plusDays(1).toString())
                        .param("serviceId", serviceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("09:00"));
    }

    @Test
    void getPublicProfessionalByIdReturnsProfessional() throws Exception {
        UUID id = UUID.randomUUID();
        when(professionalService.getPublicProfessionalById(id))
                .thenReturn(new ProfessionalResponse(id, "João", true, Set.of()));

        mockMvc.perform(get("/api/v1/public/professionals/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("João"));
    }

    @Test
    void getPublicServiceByIdReturns404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(catalogService.getPublicServiceById(id)).thenThrow(new NotFoundException("Não encontrado"));

        mockMvc.perform(get("/api/v1/public/services/" + id))
                .andExpect(status().isNotFound());
    }
}
