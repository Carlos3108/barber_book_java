package org.azdev.barber_book.services;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.ServiceRequest;
import org.azdev.barber_book.models.AppointmentService;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.repositories.AppointmentServiceRepository;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final AppointmentServiceRepository appointmentServiceRepository;
    private final TenantRepository tenantRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public AppointmentService createService(ServiceRequest dto) {
        UUID tenantId = securityUtils.getCurrentTenantId();

        Optional<AppointmentService> existingServiceOpt = appointmentServiceRepository
                .findByTenantIdAndNameIgnoreCase(tenantId, dto.name());

        if (existingServiceOpt.isPresent()) {
            AppointmentService existingService = existingServiceOpt.get();

            if (existingService.isActive()) {
                throw new IllegalArgumentException("Você já possui um serviço ativo com o nome: " + dto.name());
            } else {
                existingService.setPrice(dto.price());
                existingService.setDurationMinutes(dto.durationMinutes());
                existingService.setActive(true);
                return appointmentServiceRepository.save(existingService);
            }
        }

        Tenant tenantRef = tenantRepository.getReferenceById(tenantId);
        AppointmentService newService = new AppointmentService();
        newService.setName(dto.name());
        newService.setPrice(dto.price());
        newService.setDurationMinutes(dto.durationMinutes());
        newService.setActive(true);
        newService.setTenant(tenantRef);

        return appointmentServiceRepository.save(newService);
    }

    @Transactional(readOnly = true)
    public List<AppointmentService> listMyServices() {
        UUID tenantId = securityUtils.getCurrentTenantId();
        return appointmentServiceRepository.findAllByTenantIdAndActiveTrue(tenantId);
    }

    public void deleteService(UUID id) {
        UUID tenantId = securityUtils.getCurrentTenantId();

        AppointmentService service = appointmentServiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Serviço não encontrado."));

        if (!service.getTenant().getId().equals(tenantId)) {
            throw new AccessDeniedException("Você não tem permissão para deletar este serviço.");
        }

        service.setActive(false);

        appointmentServiceRepository.save(service);
    }

    public AppointmentService updateService (UUID id, ServiceRequest dto) {
        UUID tenantId = securityUtils.getCurrentTenantId();

        AppointmentService service = appointmentServiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Serviço não encontrado."));

        if (!service.getTenant().getId().equals(tenantId)) {
            throw new AccessDeniedException("Você não tem permissão para editar este serviço.");
        }

        if (!service.getName().equalsIgnoreCase(dto.name())) {
            Optional<AppointmentService> existingServiceOpt = appointmentServiceRepository
                    .findByTenantIdAndNameIgnoreCase(tenantId, dto.name());

            if (existingServiceOpt.isPresent() && existingServiceOpt.get().isActive()) {
                throw new IllegalArgumentException("Você já possui um serviço ativo com o nome: " + dto.name());
            }
        }

        service.setName(dto.name());
        service.setPrice(dto.price());
        service.setDurationMinutes(dto.durationMinutes());
        service.setUpdatedAt(java.time.LocalDateTime.now());

        return appointmentServiceRepository.save(service);
    }

}
