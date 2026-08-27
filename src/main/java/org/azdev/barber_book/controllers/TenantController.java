package org.azdev.barber_book.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.TenantRequest;
import org.azdev.barber_book.dtos.TenantResponse;
import org.azdev.barber_book.services.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Configurações", description = "Gerenciamento interno das configurações da barbearia")
public class TenantController {

    private final TenantService tenantService;

    @PutMapping("/my-barbershop")
    @Operation(summary = "Atualiza as informações, horários e fuso da barbearia logada")
    public ResponseEntity<Void> updateTenantSettings(@Valid @RequestBody TenantRequest request) {
        tenantService.updateMyBarbershop(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-barbershop")
    @Operation(summary = "Busca as configurações atuais da barbearia logada")
    public ResponseEntity<TenantResponse> getTenantSettings() {
        return ResponseEntity.ok(tenantService.getMyBarbershopSettings());
    }
}