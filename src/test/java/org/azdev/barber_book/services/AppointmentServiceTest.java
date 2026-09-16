package org.azdev.barber_book.services;

import org.azdev.barber_book.dtos.AppointmentRequest;
import org.azdev.barber_book.exception.ConflictException;
import org.azdev.barber_book.models.Catalog;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.repositories.AppointmentRepository;
import org.azdev.barber_book.repositories.CatalogRepository;
import org.azdev.barber_book.repositories.ProfessionalRepository;
import org.azdev.barber_book.repositories.TenantRepository;
import org.azdev.barber_book.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CatalogRepository serviceRepository;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AppointmentAvailabilityService appointmentAvailabilityService;

    @Mock
    private AppointmentBusinessRules appointmentBusinessRules;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void shouldRejectAppointmentWhenSlotIsAlreadyTaken() {
        UUID serviceId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        Catalog service = new Catalog();
        service.setId(serviceId);
        service.setActive(true);
        service.setDurationMinutes(60);
        service.setTenant(tenant);

        Professional professional = new Professional();
        professional.setId(professionalId);
        professional.setActive(true);
        professional.setTenant(tenant);

        AppointmentRequest request = new AppointmentRequest(
                "Cliente Teste",
                "11999999999",
                professionalId,
                serviceId,
                startTime
        );

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));
        when(appointmentRepository.hasOverlappingAppointment(
                professionalId,
                startTime,
                startTime.plusMinutes(60),
                List.of(org.azdev.barber_book.models.enums.AppointmentStatus.PENDING,
                        org.azdev.barber_book.models.enums.AppointmentStatus.CONFIRMED)
        )).thenReturn(true);

        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ops! Este horário acabou de ser reservado por outra pessoa. Por favor, escolha outro horário.");
    }
}
