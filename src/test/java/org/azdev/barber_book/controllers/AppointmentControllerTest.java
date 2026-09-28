package org.azdev.barber_book.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.azdev.barber_book.dtos.AppointmentResponse;
import org.azdev.barber_book.exception.GlobalExceptionHandler;
import org.azdev.barber_book.models.enums.AppointmentStatus;
import org.azdev.barber_book.services.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    @Mock
    private AppointmentService appointmentService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AppointmentController(appointmentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private record AppointmentPayload(String clientName, String clientPhone, UUID professionalId,
                                       UUID serviceId, OffsetDateTime startTime) {
    }

    @Test
    void createAppointmentReturnsCreated() throws Exception {
        UUID id = UUID.randomUUID();
        AppointmentResponse response = new AppointmentResponse(id, "Cliente", "11999999999",
                OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusMinutes(30),
                AppointmentStatus.CONFIRMED, "João", "Corte", new BigDecimal("30.00"));
        when(appointmentService.createAppointment(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AppointmentPayload(
                                "Cliente", "11999999999", UUID.randomUUID(), UUID.randomUUID(),
                                OffsetDateTime.now().plusDays(1)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientName").value("Cliente"));
    }

    @Test
    void getAvailableSlotsReturnsSlots() throws Exception {
        UUID professionalId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        when(appointmentService.getAvailableSlots(any(), any(), any())).thenReturn(List.of("09:00"));

        mockMvc.perform(get("/api/v1/appointments/availability/" + professionalId)
                        .param("date", LocalDate.now().plusDays(1).toString())
                        .param("serviceId", serviceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("09:00"));
    }

    @Test
    void listAppointmentsReturnsList() throws Exception {
        AppointmentResponse response = new AppointmentResponse(UUID.randomUUID(), "Cliente", "11999999999",
                OffsetDateTime.now(), OffsetDateTime.now().plusMinutes(30),
                AppointmentStatus.CONFIRMED, "João", "Corte", new BigDecimal("30.00"));
        when(appointmentService.listAppointmentsByTenant(any(), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/appointments")
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clientName").value("Cliente"));
    }

    @Test
    void cancelAppointmentReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(appointmentService).cancelAppointment(id);

        mockMvc.perform(patch("/api/v1/appointments/" + id + "/cancel"))
                .andExpect(status().isNoContent());

        verify(appointmentService).cancelAppointment(id);
    }

    @Test
    void completeAppointmentReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(appointmentService).completeAppointment(id);

        mockMvc.perform(patch("/api/v1/appointments/" + id + "/complete"))
                .andExpect(status().isNoContent());

        verify(appointmentService).completeAppointment(id);
    }
}
