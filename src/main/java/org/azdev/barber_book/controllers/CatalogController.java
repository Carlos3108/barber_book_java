package org.azdev.barber_book.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@RequestMapping("/api/service")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CatalogController {
    private final CatalogService catalogService;

    @PostMapping
    public ResponseEntity<CatalogResponse> create(@Valid @RequestBody CatalogRequest request){
        CatalogResponse createdService = catalogService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdService);
    }

    @GetMapping
    public ResponseEntity<List<CatalogResponse>> list(){
        return ResponseEntity.ok(catalogService.listMyServices());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CatalogResponse> delete(@PathVariable UUID id){
        catalogService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<CatalogResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody CatalogRequest request){

        return ResponseEntity.ok(catalogService.updateService(id, request));
    }
}
