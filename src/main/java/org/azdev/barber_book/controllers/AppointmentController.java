package org.azdev.barber_book.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.azdev.barber_book.dtos.ClientFrequencyReportResponse;
import org.azdev.barber_book.dtos.AppointmentRequest;
import org.azdev.barber_book.dtos.AppointmentResponse;
import org.azdev.barber_book.services.AppointmentService;
import org.azdev.barber_book.services.ClientFrequencyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Agendamentos", description = "Gerenciamento de agendamentos")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final ClientFrequencyService clientFrequencyService;

    public AppointmentController(
            AppointmentService appointmentService,
            ClientFrequencyService clientFrequencyService
    ) {
        this.appointmentService = appointmentService;
        this.clientFrequencyService = clientFrequencyService;
    }

    @PostMapping
    @Operation(summary = "Criar agendamento")
    public ResponseEntity<AppointmentResponse> createAppointment(@Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/availability/{professionalId}")
    @Operation(summary = "Verificar horários disponíveis do profissional")
    public ResponseEntity<List<String>> getAvailableSlots(
            @PathVariable UUID professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam UUID serviceId) {
        
        List<String> availableSlots = appointmentService.getAvailableSlots(professionalId, date, serviceId);
        return ResponseEntity.ok(availableSlots);
    }

    @GetMapping
    @Operation(summary = "Listar agendamentos do tenant")
    public ResponseEntity<List<AppointmentResponse>> listAppointments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AppointmentResponse> appointments = appointmentService.listAppointmentsByTenant(startDate, endDate);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/client-frequency")
    @Operation(summary = "Resumo mensal de frequência de clientes da barbearia")
    public ResponseEntity<ClientFrequencyReportResponse> getClientFrequency(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "25") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(clientFrequencyService.getReport(month, page, size));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar um agendamento")
    public ResponseEntity<Void> cancelAppointment(@PathVariable UUID id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Marcar agendamento como concluído")
    public ResponseEntity<Void> completeAppointment(@PathVariable UUID id) {
        appointmentService.completeAppointment(id);
        return ResponseEntity.noContent().build();
    }
}
