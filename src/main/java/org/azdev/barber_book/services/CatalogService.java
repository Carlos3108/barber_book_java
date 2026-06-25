package org.azdev.barber_book.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.CatalogRequest;
import org.azdev.barber_book.dtos.CatalogResponse;
import org.azdev.barber_book.models.Catalog;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.repositories.CatalogRepository;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final CatalogRepository catalogRepository;
    private final TenantRepository tenantRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public CatalogResponse createService(CatalogRequest dto) {
        UUID tenantId = securityUtils.getCurrentTenantId();

        Optional<Catalog> existingServiceOpt = catalogRepository
                .findByTenantIdAndNameIgnoreCase(tenantId, dto.name());

        if (existingServiceOpt.isPresent()) {
            Catalog existingService = existingServiceOpt.get();

            if (existingService.isActive()) {
                throw new IllegalArgumentException("Você já possui um serviço ativo com o nome: " + dto.name());
            } else {
                existingService.setPrice(dto.price());
                existingService.setDurationMinutes(dto.durationMinutes());
                existingService.setActive(true);

                Catalog savedExisting = catalogRepository.save(existingService);
                return mapToResponse(savedExisting);
            }
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Barbearia não encontrada."));

        Catalog newCatalog = new Catalog();
        newCatalog.setName(dto.name());
        newCatalog.setPrice(dto.price());
        newCatalog.setDurationMinutes(dto.durationMinutes());
        newCatalog.setActive(true);
        newCatalog.setTenant(tenant);

        Catalog savedService = catalogRepository.save(newCatalog);

        return mapToResponse(savedService);
    }

    @Transactional(readOnly = true)
    public List<CatalogResponse> listMyServices() {
        UUID tenantId = securityUtils.getCurrentTenantId();
        return catalogRepository.findAllByTenantIdAndActiveTrue(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void deleteService(UUID id) {
        UUID tenantId = securityUtils.getCurrentTenantId();

        Catalog catalog = getCatalogAndValidateOwner(id, tenantId);

        catalog.setActive(false);
        catalogRepository.save(catalog);
    }

    public CatalogResponse updateService (UUID id, CatalogRequest dto) {
        UUID tenantId = securityUtils.getCurrentTenantId();

        Catalog catalog = getCatalogAndValidateOwner(id, tenantId);

        if (!catalog.getName().equalsIgnoreCase(dto.name())) {
            Optional<Catalog> existingServiceOpt = catalogRepository
                    .findByTenantIdAndNameIgnoreCase(tenantId, dto.name());

            if (existingServiceOpt.isPresent() && existingServiceOpt.get().isActive()) {
                throw new IllegalArgumentException("Você já possui um serviço ativo com o nome: " + dto.name());
            }
        }

        catalog.setName(dto.name());
        catalog.setPrice(dto.price());
        catalog.setDurationMinutes(dto.durationMinutes());
        catalog.setUpdatedAt(OffsetDateTime.from(LocalDateTime.now()));

        return mapToResponse(catalogRepository.save(catalog));
    }

    private CatalogResponse mapToResponse(Catalog catalog){
        return new CatalogResponse(
                catalog.getId(),
                catalog.getName(),
                catalog.getPrice(),
                catalog.getDurationMinutes(),
                catalog.isActive()
        );
    }

    private Catalog getCatalogAndValidateOwner(UUID catalogId, UUID tenantId){
        Catalog catalog = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        if (!catalog.getTenant().getId().equals(tenantId)) {
            throw new AccessDeniedException("Acesso negado: Você não tem permissão para alterar este serviço.");
        }

        return catalog;
    }
}
