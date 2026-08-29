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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

        Catalog catalogToSave = handleSmartUpsert(dto, tenantId);

        catalogToSave = catalogRepository.save(catalogToSave);

        return mapToResponse(catalogToSave);
    }

    @Transactional(readOnly = true)
    public List<CatalogResponse> listMyServices() {
        UUID tenantId = securityUtils.getCurrentTenantId();
        return catalogRepository.findAllByTenantIdAndActiveTrue(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CatalogResponse getMyServiceById(UUID id) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        Catalog catalog = getCatalogAndValidateOwner(id, tenantId);
        return mapToResponse(catalog);
    }

    @Transactional(readOnly = true)
    public CatalogResponse getPublicServiceById(UUID id) {
        Catalog catalog = catalogRepository.findById(id)
                .filter(Catalog::isActive)
                .orElseThrow(() -> new EntityNotFoundException("Serviço não encontrado ou indisponível."));
        return mapToResponse(catalog);
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
        catalog.setUpdatedAt(OffsetDateTime.now());

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

        return catalogRepository.findByIdAndTenantId(catalogId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Serviço não encontrado ou não pertence ao seu estabelecimento."));
    }

    public List<CatalogResponse> getPublicServicesBySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Barbearia não encontrada."));

        return catalogRepository.findAllByTenantIdAndActiveTrue(tenant.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Catalog handleSmartUpsert(CatalogRequest request, UUID tenantId) {
        Optional<Catalog> existingOpt = catalogRepository.findByTenantIdAndNameIgnoreCase(tenantId, request.name());

        if (existingOpt.isPresent()) {
            Catalog existingCatalog = existingOpt.get();

            if (existingCatalog.isActive()) {
                throw new IllegalArgumentException("Já existe um serviço ativo cadastrado com este nome.");
            }

            existingCatalog.setActive(true);
            existingCatalog.setPrice(request.price());
            existingCatalog.setDurationMinutes(request.durationMinutes());

            return existingCatalog;
        }

        Catalog newCatalog = new Catalog();
        newCatalog.setName(request.name());
        newCatalog.setPrice(request.price());
        newCatalog.setDurationMinutes(request.durationMinutes());
        newCatalog.setActive(true);

        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        newCatalog.setTenant(tenant);

        return newCatalog;
    }
}
