package org.azdev.barber_book.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.AppointmentRequest;
import org.azdev.barber_book.dtos.AppointmentResponse;
import org.azdev.barber_book.dtos.CatalogResponse;
import org.azdev.barber_book.dtos.ProfessionalResponse;
import org.azdev.barber_book.services.AppointmentService;
import org.azdev.barber_book.services.CatalogService;
import org.azdev.barber_book.services.ProfessionalService;
import org.azdev.barber_book.services.TenantService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Público", description = "Endpoints públicos para clientes da barbearia")
public class PublicController {

    // O Controller SÓ conversa com os Services! Zero Repositories aqui.
    private final TenantService tenantService;
    private final CatalogService catalogService;
    private final ProfessionalService professionalService;
    private final AppointmentService appointmentService;

    @GetMapping("/barbershop/{slug}")
    @Operation(summary = "Busca informações básicas de uma barbearia pelo seu slug")
    public ResponseEntity<Map<String, String>> getBarbershopInfo(@PathVariable String slug) {
        return ResponseEntity.ok(tenantService.getPublicInfoBySlug(slug));
    }

    @GetMapping("/barbershop/{slug}/services")
    @Operation(summary = "Lista os serviços ativos de uma barbearia")
    public ResponseEntity<List<CatalogResponse>> getBarbershopServices(@PathVariable String slug) {
        return ResponseEntity.ok(catalogService.getPublicServicesBySlug(slug));
    }

    @GetMapping("/barbershop/{slug}/professionals")
    @Operation(summary = "Lista os profissionais ativos de uma barbearia")
    public ResponseEntity<List<ProfessionalResponse>> getBarbershopProfessionals(@PathVariable String slug) {
        return ResponseEntity.ok(professionalService.getPublicProfessionalsBySlug(slug));
    }

    @GetMapping("/professionals/{professionalId}/slots")
    @Operation(summary = "Retorna os horários disponíveis de um profissional para uma data específica")
    public ResponseEntity<List<String>> getProfessionalSlots(
            @PathVariable UUID professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam UUID serviceId
    ) {
        List<String> slots = appointmentService.getAvailableSlots(professionalId, date, serviceId);
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/appointments")
    @Operation(summary = "Cria um novo agendamento para um cliente (público)")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody AppointmentRequest request
    ) {
        AppointmentResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}