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
        Tenant tenantRef = tenantRepository.getReferenceById(tenantId);

        AppointmentService service = new AppointmentService();
        service.setName(dto.name());
        service.setPrice(dto.price());
        service.setDurationMinutes(dto.durationMinutes());
        service.setTenant(tenantRef);

        return appointmentServiceRepository.save(service);
    }

    @Transactional(readOnly = true)
    public List<AppointmentService> listMyServices() {
        UUID tenantId = securityUtils.getCurrentTenantId();
        return appointmentServiceRepository.findAllByTenantId(tenantId);
    }

    public void deleteService(UUID id) {
        AppointmentService service = appointmentServiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service not found"));
        if (!service.getTenant().getId().equals(securityUtils.getCurrentTenantId())) {
            throw new AccessDeniedException("Access denied");
        }
        appointmentServiceRepository.delete(service);
    }

}
