package org.azdev.barber_book.services;

import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.ProfessionalRequest;
import org.azdev.barber_book.dtos.ProfessionalResponse;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.repositories.ProfessionalRepository;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

    private final ProfessionalRepository repository;
    private final TenantRepository tenantRepository;
    private final SecurityUtils  securityUtils;

    @Transactional
    public ProfessionalResponse createProfessional(ProfessionalRequest dto) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        Professional professional = handleSmartUpsert(dto, tenantId);
        return mapToResponse(repository.save(professional));
    }

    @Transactional(readOnly = true)
    public List<ProfessionalResponse> listMyProfessionals() {
        UUID tenantId = securityUtils.getCurrentTenantId();
        return repository.findAllByTenantIdAndActiveTrue(tenantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProfessionalResponse updateProfessional(UUID id, ProfessionalRequest dto) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        Professional professional = getProfessionalAndValidateOwnership(id, tenantId);

        professional.setName(dto.name());

        return mapToResponse(repository.save(professional));
    }

    @Transactional
    public void deleteProfessional(UUID id) {
        UUID tenantId = securityUtils.getCurrentTenantId();
        Professional professional = getProfessionalAndValidateOwnership(id, tenantId);

        professional.setActive(false);
        repository.save(professional);
    }

    private Professional getProfessionalAndValidateOwnership(UUID professionalId, UUID tenantId) {
        return repository.findByIdAndTenantId(professionalId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado ou acesso negado."));
    }

    private Professional handleSmartUpsert(ProfessionalRequest request, UUID tenantId) {
        Optional<Professional> existingOpt = repository.findByTenantIdAndNameIgnoreCase(tenantId, request.name());

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

    private ProfessionalResponse mapToResponse(Professional professional) {
        return new ProfessionalResponse(
                professional.getId(),
                professional.getName(),
                professional.isActive()
        );
    }
}
