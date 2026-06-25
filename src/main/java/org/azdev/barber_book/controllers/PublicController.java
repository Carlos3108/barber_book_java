package org.azdev.barber_book.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.AppointmentRequest;
import org.azdev.barber_book.dtos.AppointmentResponse;
import org.azdev.barber_book.models.AppointmentService;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.repositories.AppointmentRepository;
import org.azdev.barber_book.repositories.AppointmentServiceRepository;
import org.azdev.barber_book.repositories.ProfessionalRepository;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.services.AppointmentServiceService;
import org.azdev.barber_book.services.ProfessionalService;
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
public class PublicController {

    private final TenantRepository tenantRepository;
    private final AppointmentServiceRepository serviceRepository;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentServiceService appointmentServiceService;

    @GetMapping("/barbershop/{slug}")
    public ResponseEntity<?> getBarbershopInfo(@PathVariable String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Barbearia não encontrada."));

        return ResponseEntity.ok(Map.of(
                "id", tenant.getId(),
                "name", tenant.getName()));
    }

    @GetMapping("/barbershop/{slug}/services")
    public ResponseEntity<?> getBarbershopServices(@PathVariable String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Barbearia não encontrada."));

        var services = serviceRepository.findAllByTenantIdAndActiveTrue(tenant.getId());
        return ResponseEntity.ok(services);
    }

    @GetMapping("/barbershop/{slug}/professionals")
    public ResponseEntity<?> getBarbershopProfessionals(@PathVariable String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Barbearia não encontrada."));

        var professionals = professionalRepository.findAllByTenantIdAndActiveTrue(tenant.getId());
        return ResponseEntity.ok(professionals);
    }

    @GetMapping("/professionals/{professionalId}/slots")
    public ResponseEntity<List<String>> getProfessionalSlots(
            @PathVariable UUID professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<String> slots = appointmentServiceService.getAvailableSlots(professionalId, date);
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/appointments")
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody AppointmentRequest request
    ) {
        AppointmentResponse response = appointmentServiceService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
