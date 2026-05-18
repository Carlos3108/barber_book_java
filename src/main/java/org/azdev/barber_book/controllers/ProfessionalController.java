package org.azdev.barber_book.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
public class ProfessionalController {

    private final ProfessionalService professionalService;

    @PostMapping
    public ResponseEntity<ProfessionalResponse> create(
            @Valid @RequestBody ProfessionalRequest request
    ) {
        ProfessionalResponse created = professionalService.createProfessional(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ProfessionalResponse>> list() {
        return ResponseEntity.ok(professionalService.listMyProfessionals());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfessionalResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProfessionalRequest request
    ) {
        return ResponseEntity.ok(professionalService.updateProfessional(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        professionalService.deleteProfessional(id);
        return ResponseEntity.noContent().build();
    }
}
