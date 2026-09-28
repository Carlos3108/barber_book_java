package org.azdev.barber_book.services;

import org.azdev.barber_book.dtos.AppointmentRequest;
import org.azdev.barber_book.dtos.AppointmentResponse;
import org.azdev.barber_book.exception.ConflictException;
import org.azdev.barber_book.exception.NotFoundException;
import org.azdev.barber_book.models.Appointment;
import org.azdev.barber_book.models.Catalog;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.enums.AppointmentStatus;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void shouldCreateAppointmentWhenSlotIsFree() {
        UUID serviceId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1);

        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        Catalog service = new Catalog();
        service.setId(serviceId);
        service.setName("Corte");
        service.setActive(true);
        service.setDurationMinutes(60);
        service.setPrice(new BigDecimal("50.00"));
        service.setTenant(tenant);

        Professional professional = new Professional();
        professional.setId(professionalId);
        professional.setName("João");
        professional.setActive(true);
        professional.setTenant(tenant);

        AppointmentRequest request = new AppointmentRequest(
                "Cliente Teste", "11999999999", professionalId, serviceId, startTime);

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
        when(professionalRepository.findById(professionalId)).thenReturn(Optional.of(professional));
        when(appointmentRepository.hasOverlappingAppointment(any(), any(), any(), any())).thenReturn(false);
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = appointmentService.createAppointment(request);

        assertThat(response.clientName()).isEqualTo("Cliente Teste");
        assertThat(response.professionalName()).isEqualTo("João");
        assertThat(response.serviceName()).isEqualTo("Corte");
    }

    @Test
    void shouldThrowWhenServiceNotFound() {
        UUID serviceId = UUID.randomUUID();
        AppointmentRequest request = new AppointmentRequest(
                "Cliente", "11999999999", UUID.randomUUID(), serviceId, OffsetDateTime.now().plusDays(1));

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldThrowWhenProfessionalNotFound() {
        UUID serviceId = UUID.randomUUID();
        UUID professionalId = UUID.randomUUID();
        Catalog service = new Catalog();
        service.setId(serviceId);

        AppointmentRequest request = new AppointmentRequest(
                "Cliente", "11999999999", professionalId, serviceId, OffsetDateTime.now().plusDays(1));

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
        when(professionalRepository.findById(professionalId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAvailableSlotsDelegatesToAvailabilityService() {
        UUID professionalId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);

        when(appointmentAvailabilityService.getAvailableSlots(professionalId, date, serviceId))
                .thenReturn(List.of("09:00", "09:30"));

        List<String> slots = appointmentService.getAvailableSlots(professionalId, date, serviceId);

        assertThat(slots).containsExactly("09:00", "09:30");
    }

    @Test
    void listAppointmentsByTenantReturnsMappedAppointments() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setTimezone("America/Sao_Paulo");

        Professional professional = new Professional();
        professional.setId(UUID.randomUUID());
        professional.setName("João");

        Catalog service = new Catalog();
        service.setId(UUID.randomUUID());
        service.setName("Corte");
        service.setPrice(new BigDecimal("30.00"));

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setClientName("Cliente");
        appointment.setClientPhone("11999999999");
        appointment.setStartTime(OffsetDateTime.now());
        appointment.setEndTime(OffsetDateTime.now().plusMinutes(30));
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setProfessional(professional);
        appointment.setService(service);
        appointment.setTenant(tenant);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(appointmentRepository.findByTenantIdAndStartTimeBetweenWithDetails(any(), any(), any()))
                .thenReturn(List.of(appointment));

        List<AppointmentResponse> result = appointmentService.listAppointmentsByTenant(
                LocalDate.now(), LocalDate.now().plusDays(1));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().clientName()).isEqualTo("Cliente");
    }

    @Test
    void listAppointmentsByTenantThrowsWhenTenantMissing() {
        UUID tenantId = UUID.randomUUID();
        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.listAppointmentsByTenant(
                LocalDate.now(), LocalDate.now().plusDays(1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void cancelAppointmentUpdatesStatusToCancelled() {
        UUID tenantId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        Appointment appointment = new Appointment();
        appointment.setId(appointmentId);
        appointment.setTenant(tenant);
        appointment.setStatus(AppointmentStatus.PENDING);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appointmentService.cancelAppointment(appointmentId);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    void cancelAppointmentThrowsWhenNotFound() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.cancelAppointment(appointmentId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void completeAppointmentUpdatesStatusToCompleted() {
        UUID tenantId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);

        Appointment appointment = new Appointment();
        appointment.setId(appointmentId);
        appointment.setTenant(tenant);
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        when(securityUtils.getCurrentTenantId()).thenReturn(tenantId);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appointmentService.completeAppointment(appointmentId);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    void completeAppointmentThrowsWhenNotFound() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.completeAppointment(appointmentId))
                .isInstanceOf(NotFoundException.class);
    }
}
