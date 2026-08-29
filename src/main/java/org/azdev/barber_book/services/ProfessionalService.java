package org.azdev.barber_book.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.ProfessionalRequest;
import org.azdev.barber_book.dtos.ProfessionalResponse;
import org.azdev.barber_book.models.Catalog;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.repositories.CatalogRepository;
import org.azdev.barber_book.repositories.ProfessionalRepository;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

    private final ProfessionalRepository professionalRepository;
    private final TenantRepository tenantRepository;
    private final SecurityUtils  securityUtils;
    private final CatalogRepository catalogRepository;

    @Transactional
    public ProfessionalResponse createProfessional(ProfessionalRequest dto) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        Professional professional = handleSmartUpsert(dto, tenantId);
        linkServicesToProfessional(professional, dto.serviceIds(), tenantId);
        return mapToResponse(professionalRepository.save(professional));
    }

    @Transactional(readOnly = true)
    public List<ProfessionalResponse> listMyProfessionals() {
        UUID tenantId = securityUtils.getCurrentTenantId();
        return professionalRepository.findAllByTenantIdAndActiveTrue(tenantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProfessionalResponse updateProfessional(UUID id, ProfessionalRequest dto) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        Professional professional = getProfessionalAndValidateOwnership(id, tenantId);

        professional.setName(dto.name());
        linkServicesToProfessional(professional, dto.serviceIds(), tenantId);

        return mapToResponse(professionalRepository.save(professional));
    }

    @Transactional
    public void deleteProfessional(UUID id) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        Professional professional = getProfessionalAndValidateOwnership(id, tenantId);

        professional.setActive(false);
        professionalRepository.save(professional);
    }

    public List<ProfessionalResponse> getPublicProfessionalsBySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Barbearia não encontrada."));

        return professionalRepository.findAllByTenantIdAndActiveTrue(tenant.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Professional getProfessionalAndValidateOwnership(UUID professionalId, UUID tenantId) {
        return professionalRepository.findByIdAndTenantId(professionalId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado ou acesso negado."));
    }

    private Professional handleSmartUpsert(ProfessionalRequest request, UUID tenantId) {
        Optional<Professional> existingOpt = professionalRepository.findByTenantIdAndNameIgnoreCase(tenantId, request.name());

        if (existingOpt.isPresent()) {
            Professional existing = existingOpt.get();
            if (existing.isActive()) {
                throw new IllegalArgumentException("Já existe um profissional ativo com este nome.");
            }

            existing.setActive(true);
            existing.setName(request.name());
            return existing;
        }

        Professional newProfessional = new Professional();
        newProfessional.setName(request.name());
        newProfessional.setActive(true);
        newProfessional.setTenant(tenantRepository.getReferenceById(tenantId));

        return newProfessional;
    }

    private void linkServicesToProfessional(Professional professional, Set<UUID> serviceIds, UUID tenantId){
        professional.getServices().clear();

        if (serviceIds != null && !serviceIds.isEmpty()) {
            List<Catalog> validServices = catalogRepository.findAllById(serviceIds);

            for (Catalog service : validServices) {
                if (!service.getTenant().getId().equals(tenantId)) {
                    throw new SecurityException("Inconsistência: Tentativa de vincular um serviço de outra barbearia.");
                }
                professional.getServices().add(service);
            }
        }
    }

    private ProfessionalResponse mapToResponse(Professional professional) {
        Set<UUID> serviceIds = professional.getServices().stream()
                .map(Catalog::getId)
                .collect(Collectors.toSet());

        return new ProfessionalResponse(
                professional.getId(),
                professional.getName(),
                professional.isActive(),
                serviceIds
        );
    }
}
