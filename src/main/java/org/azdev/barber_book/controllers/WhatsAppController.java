package org.azdev.barber_book.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.azdev.barber_book.dtos.WhatsAppSettingsRequest;
import org.azdev.barber_book.dtos.WhatsAppStatusResponse;
import org.azdev.barber_book.services.WhatsAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    @GetMapping("/status")
    public ResponseEntity<WhatsAppStatusResponse> getStatus() {
        return ResponseEntity.ok(whatsAppService.getSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<Void> updateSettings(@Valid @RequestBody WhatsAppSettingsRequest request) {
        whatsAppService.updateSettings(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/qrcode")
    public ResponseEntity<Map<String, String>> generateQrCode() {
        String base64 = whatsAppService.generateQrCode();
        return ResponseEntity.ok(Map.of("qrCodeBase64", base64));
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect() {
        whatsAppService.disconnect();
        return ResponseEntity.noContent().build();
    }
}