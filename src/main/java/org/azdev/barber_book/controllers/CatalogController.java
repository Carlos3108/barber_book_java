package org.azdev.barber_book.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.ServiceRequest;
import org.azdev.barber_book.models.AppointmentService;
import org.azdev.barber_book.services.CatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service")
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalogService;

    @PostMapping
    public ResponseEntity<AppointmentService> create(@Valid @RequestBody ServiceRequest request){
        AppointmentService createdService = catalogService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdService);
    }

    @GetMapping
    public ResponseEntity<List<AppointmentService>> list(){
        return ResponseEntity.ok(catalogService.listMyServices());
    }
}
