package org.azdev.barber_book.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.ServiceRequest;
import org.azdev.barber_book.dtos.ServiceResponse;
import org.azdev.barber_book.models.AppointmentService;
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
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody ServiceRequest request){
        AppointmentService createdService = catalogService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ServiceResponse(
                createdService.getId(),
                createdService.getName(),
                createdService.getPrice(),
                createdService.getDurationMinutes(),
                createdService.getTenant().getId()
        ));
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> list(){
        return ResponseEntity.ok(catalogService.listMyServices().stream()
            .map(service -> new ServiceResponse(
                    service.getId(),
                    service.getName(),
                    service.getPrice(),
                    service.getDurationMinutes(),
                    service.getTenant().getId()
            )).toList()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ServiceResponse> delete(@PathVariable UUID id){
        catalogService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}
