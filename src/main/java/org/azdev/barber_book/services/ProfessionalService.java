package org.azdev.barber_book.services;

import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.ProfessionalRequest;
import org.azdev.barber_book.dtos.ProfessionalResponse;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.models.Tenant;
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
        Optional<Professional> existingOpt = repository.findByTenantIdAndNameIgnoreCase(tenantId, dto.name());

        if (existingOpt.isPresent()) {
            Professional existing = existingOpt.get();
            if (existing.isActive()) {
                throw new IllegalArgumentException("Você já possui um profissional ativo com o nome: " + dto.name());
            } else {
                existing.setActive(true);
                return mapToResponse(repository.save(existing));
            }
        }

        Tenant tenantRef = tenantRepository.getReferenceById(tenantId);
        Professional professional = new Professional();
        professional.setName(dto.name());
        professional.setActive(true);
        professional.setTenant(tenantRef);

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

        if (!professional.getName().equalsIgnoreCase(dto.name())) {
            boolean nameExists = repository.findByTenantIdAndNameIgnoreCase(tenantId, dto.name())
                    .filter(Professional::isActive)
                    .isPresent();
            if (nameExists) {
                throw new IllegalArgumentException("Já existe outro profissional ativo com este nome.");
            }
        }

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
        Professional professional = repository.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado."));

        if (!professional.getTenant().getId().equals(tenantId)) {
            throw new SecurityException("Acesso negado.");
        }
        return professional;
    }

    private ProfessionalResponse mapToResponse(Professional professional) {
        return new ProfessionalResponse(
                professional.getId(),
                professional.getName(),
                professional.isActive()
        );
    }
}
