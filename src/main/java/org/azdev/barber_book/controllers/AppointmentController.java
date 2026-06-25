package org.azdev.barber_book.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.AppointmentRequest;
import org.azdev.barber_book.dtos.AppointmentResponse;
import org.azdev.barber_book.services.AppointmentServiceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Agendamentos", description = "Gerenciamento de agendamentos")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentServiceService appointmentServiceService;

    @PostMapping
    @Operation(summary = "Criar agendamento")
    public ResponseEntity<AppointmentResponse> createAppointment(@Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse response = appointmentServiceService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/availability/{professionalId}")
    @Operation(summary = "Verificar horários disponíveis do profissional")
    public ResponseEntity<List<String>> getAvailableSlots(
            @PathVariable UUID professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<String> availableSlots = appointmentServiceService.getAvailableSlots(professionalId, date);
        return ResponseEntity.ok(availableSlots);
    }

    @GetMapping
    @Operation(summary = "Listar agendamentos do tenant")
    public ResponseEntity<List<AppointmentResponse>> listAppointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AppointmentResponse> appointments = appointmentServiceService.listAppointmentsByTenant(startDate, endDate);
        return ResponseEntity.ok(appointments);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar um agendamento")
    public ResponseEntity<Void> cancelAppointment(@PathVariable UUID id) {
        appointmentServiceService.cancelAppointment(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Marcar agendamento como concluído")
    public ResponseEntity<Void> completeAppointment(@PathVariable UUID id) {
        appointmentServiceService.completeAppointment(id);
        return ResponseEntity.noContent().build();
    }
}
