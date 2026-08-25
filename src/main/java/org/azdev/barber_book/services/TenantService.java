package org.azdev.barber_book.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.repositories.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    public Map<String, String> getPublicInfoBySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Barbearia não encontrada."));

        return Map.of("name", tenant.getName());
    }
}