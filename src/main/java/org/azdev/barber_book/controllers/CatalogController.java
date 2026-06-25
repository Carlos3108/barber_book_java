package org.azdev.barber_book.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.CatalogRequest;
import org.azdev.barber_book.dtos.CatalogResponse;
import org.azdev.barber_book.services.CatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Catálogo de Serviços", description = "Endpoints para gerenciamento dos serviços da barbearia")
public class CatalogController {
    private final CatalogService catalogService;

    @PostMapping
    @Operation(summary = "Cria um novo serviço no catálogo")
    public ResponseEntity<CatalogResponse> create(@Valid @RequestBody CatalogRequest request){
        CatalogResponse createdService = catalogService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdService);
    }

    @GetMapping
    @Operation(summary = "Lista todos os serviços ativos da barbearia")
    public ResponseEntity<List<CatalogResponse>> list(){
        return ResponseEntity.ok(catalogService.listMyServices());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativa (soft delete) um serviço do catálogo")
    public ResponseEntity<Void> delete(@PathVariable UUID id){
        catalogService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um serviço existente no catálogo")
    public ResponseEntity<CatalogResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody CatalogRequest request){

        return ResponseEntity.ok(catalogService.updateService(id, request));
    }
}