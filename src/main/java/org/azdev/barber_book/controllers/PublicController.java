package org.azdev.barber_book.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.AppointmentRequest;
import org.azdev.barber_book.dtos.AppointmentResponse;
import org.azdev.barber_book.dtos.CatalogResponse;
import org.azdev.barber_book.dtos.ProfessionalResponse;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.repositories.CatalogRepository;
import org.azdev.barber_book.repositories.ProfessionalRepository;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.services.AppointmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Público", description = "Endpoints públicos para clientes da barbearia")
public class PublicController {

    private final TenantRepository tenantRepository;
    private final CatalogRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentService appointmentService;

    @GetMapping("/barbershop/{slug}")
    @Operation(summary = "Busca informações básicas de uma barbearia pelo seu slug")
    public ResponseEntity<?> getBarbershopInfo(@PathVariable String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Barbearia não encontrada."));

        return ResponseEntity.ok(Map.of(
                "id", tenant.getId(),
                "name", tenant.getName()));
    }

    @GetMapping("/barbershop/{slug}/services")
    @Operation(summary = "Lista os serviços ativos de uma barbearia")
    public ResponseEntity<?> getBarbershopServices(@PathVariable String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Barbearia não encontrada."));

        List<CatalogResponse> services = serviceRepository.findAllByTenantIdAndActiveTrue(tenant.getId())
                .stream()
                .map(catalog -> new CatalogResponse(
                        catalog.getId(),
                        catalog.getName(),
                        catalog.getPrice(),
                        catalog.getDurationMinutes(),
                        catalog.isActive()
                )).toList();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/barbershop/{slug}/professionals")
    @Operation(summary = "Lista os profissionais ativos de uma barbearia")
    public ResponseEntity<?> getBarbershopProfessionals(@PathVariable String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Barbearia não encontrada."));

        List<ProfessionalResponse> professionals = professionalRepository.findAllByTenantIdAndActiveTrue(tenant.getId())
                .stream()
                .map(professional -> new ProfessionalResponse(
                        professional.getId(),
                        professional.getName(),
                        professional.isActive()
                )).toList();
        return ResponseEntity.ok(professionals);
    }

    @GetMapping("/professionals/{professionalId}/slots")
    @Operation(summary = "Retorna os horários disponíveis de um profissional para uma data específica")
    public ResponseEntity<List<String>> getProfessionalSlots(
            @PathVariable UUID professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<String> slots = appointmentService.getAvailableSlots(professionalId, date);
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/appointments")
    @Operation(summary = "Cria um novo agendamento para um cliente")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody AppointmentRequest request
    ) {
        AppointmentResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}