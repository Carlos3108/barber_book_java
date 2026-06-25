package org.azdev.barber_book.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.ProfessionalRequest;
import org.azdev.barber_book.dtos.ProfessionalResponse;
import org.azdev.barber_book.services.ProfessionalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Profissionais", description = "Endpoints para gerenciamento dos profissionais da barbearia")
public class ProfessionalController {

    private final ProfessionalService professionalService;

    @PostMapping
    @Operation(summary = "Cria um novo profissional")
    public ResponseEntity<ProfessionalResponse> create(
            @Valid @RequestBody ProfessionalRequest request
    ) {
        ProfessionalResponse created = professionalService.createProfessional(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "Lista todos os profissionais ativos da barbearia")
    public ResponseEntity<List<ProfessionalResponse>> list() {
        return ResponseEntity.ok(professionalService.listMyProfessionals());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um profissional existente")
    public ResponseEntity<ProfessionalResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProfessionalRequest request
    ) {
        return ResponseEntity.ok(professionalService.updateProfessional(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativa (soft delete) um profissional")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        professionalService.deleteProfessional(id);
        return ResponseEntity.noContent().build();
    }
}