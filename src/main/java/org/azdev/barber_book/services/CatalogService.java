package org.azdev.barber_book.services;

import lombok.RequiredArgsConstructor;
import org.azdev.barber_book.dtos.ServiceRequest;
import org.azdev.barber_book.models.AppointmentService;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.repositories.AppointmentServiceRepository;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.security.SecurityUtils;
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
}
