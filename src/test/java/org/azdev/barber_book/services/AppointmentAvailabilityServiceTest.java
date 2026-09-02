package org.azdev.barber_book.services;

import org.azdev.barber_book.exception.BadRequestException;
import org.azdev.barber_book.exception.NotFoundException;
import org.azdev.barber_book.models.Appointment;
import org.azdev.barber_book.models.Catalog;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.enums.AppointmentStatus;
import org.azdev.barber_book.repositories.AppointmentRepository;
import org.azdev.barber_book.repositories.CatalogRepository;
import org.azdev.barber_book.repositories.ProfessionalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentAvailabilityServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CatalogRepository catalogRepository;

    @Mock
    private ProfessionalRepository professionalRepository;

    @Test
    void shouldReturnEmptyWhenRequestedDateIsInThePast() {
        UUID professionalId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Clock fixedClock = Clock.fixed(
                OffsetDateTime.parse("2026-09-01T12:00:00-03:00").toInstant(),
                ZoneId.of("America/Sao_Paulo")
        );

        AppointmentAvailabilityService service = new AppointmentAvailabilityService(
                appointmentRepository,
                catalogRepository,
                professionalRepository,
                fixedClock
        );

        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setTimezone("America/Sao_Paulo");
        tenant.setOpeningTime(LocalTime.of(9, 0));
        tenant.setClosingTime(LocalTime.of(20, 0));
        tenant.setSlotInterval(30);

        Professional professional = new Professional();
        professional.setId(professionalId);
        professional.setTenant(tenant);

        Catalog serviceEntity = new Catalog();
        serviceEntity.setId(serviceId);
        serviceEntity.setDurationMinutes(30);

        when(catalogRepository.findById(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(professionalRepository.findByIdWithTenant(professionalId)).thenReturn(Optional.of(professional));

        List<String> result = service.getAvailableSlots(professionalId, LocalDate.of(2026, 9, 1).minusDays(1), serviceId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenBusinessDayAlreadyClosed() {
        UUID professionalId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Clock fixedClock = Clock.fixed(
                OffsetDateTime.parse("2026-09-01T23:53:00-03:00").toInstant(),
                ZoneId.of("America/Sao_Paulo")
        );

        AppointmentAvailabilityService service = new AppointmentAvailabilityService(
                appointmentRepository,
                catalogRepository,
                professionalRepository,
                fixedClock
        );

        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setTimezone("America/Sao_Paulo");
        tenant.setOpeningTime(LocalTime.of(9, 0));
        tenant.setClosingTime(LocalTime.of(20, 0));
        tenant.setSlotInterval(30);

        Professional professional = new Professional();
        professional.setId(professionalId);
        professional.setTenant(tenant);

        Catalog serviceEntity = new Catalog();
        serviceEntity.setId(serviceId);
        serviceEntity.setDurationMinutes(30);

        when(catalogRepository.findById(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(professionalRepository.findByIdWithTenant(professionalId)).thenReturn(Optional.of(professional));

        List<String> result = service.getAvailableSlots(professionalId, LocalDate.of(2026, 9, 1), serviceId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotReturnSlotsBlockedByExistingAppointments() {
        UUID professionalId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Clock fixedClock = Clock.fixed(
                OffsetDateTime.parse("2026-09-01T08:00:00-03:00").toInstant(),
                ZoneId.of("America/Sao_Paulo")
        );

        AppointmentAvailabilityService service = new AppointmentAvailabilityService(
                appointmentRepository,
                catalogRepository,
                professionalRepository,
                fixedClock
        );

        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setTimezone("America/Sao_Paulo");
        tenant.setOpeningTime(LocalTime.of(9, 0));
        tenant.setClosingTime(LocalTime.of(20, 0));
        tenant.setSlotInterval(30);

        Professional professional = new Professional();
        professional.setId(professionalId);
        professional.setTenant(tenant);

        Catalog serviceEntity = new Catalog();
        serviceEntity.setId(serviceId);
        serviceEntity.setDurationMinutes(60);

        Appointment appointment = new Appointment();
        appointment.setStartTime(OffsetDateTime.parse("2026-09-01T10:00:00-03:00"));
        appointment.setEndTime(OffsetDateTime.parse("2026-09-01T11:00:00-03:00"));
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        when(catalogRepository.findById(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(professionalRepository.findByIdWithTenant(professionalId)).thenReturn(Optional.of(professional));
        when(appointmentRepository.findDailyAgendaForProfessional(
                professionalId,
                OffsetDateTime.parse("2026-09-01T00:00:00-03:00"),
                OffsetDateTime.parse("2026-09-01T23:59:59-03:00"),
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED)
        )).thenReturn(List.of(appointment));

        List<String> result = service.getAvailableSlots(professionalId, LocalDate.of(2026, 9, 1), serviceId);

        assertThat(result).doesNotContain("10:00:00");
    }

    @Test
    void shouldThrowWhenServiceDoesNotExist() {
        UUID professionalId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Clock fixedClock = Clock.fixed(
                OffsetDateTime.parse("2026-09-01T12:00:00-03:00").toInstant(),
                ZoneId.of("America/Sao_Paulo")
        );

        AppointmentAvailabilityService service = new AppointmentAvailabilityService(
                appointmentRepository,
                catalogRepository,
                professionalRepository,
                fixedClock
        );

        when(catalogRepository.findById(serviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAvailableSlots(professionalId, LocalDate.of(2026, 9, 1), serviceId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Serviço não encontrado.");
    }

    @Test
    void shouldThrowWhenServiceDurationIsInvalid() {
        UUID professionalId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Clock fixedClock = Clock.fixed(
                OffsetDateTime.parse("2026-09-01T12:00:00-03:00").toInstant(),
                ZoneId.of("America/Sao_Paulo")
        );

        AppointmentAvailabilityService service = new AppointmentAvailabilityService(
                appointmentRepository,
                catalogRepository,
                professionalRepository,
                fixedClock
        );

        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setTimezone("America/Sao_Paulo");
        tenant.setOpeningTime(LocalTime.of(9, 0));
        tenant.setClosingTime(LocalTime.of(20, 0));
        tenant.setSlotInterval(30);

        Professional professional = new Professional();
        professional.setId(professionalId);
        professional.setTenant(tenant);

        Catalog serviceEntity = new Catalog();
        serviceEntity.setId(serviceId);
        serviceEntity.setDurationMinutes(0);

        when(catalogRepository.findById(serviceId)).thenReturn(Optional.of(serviceEntity));
        when(professionalRepository.findByIdWithTenant(professionalId)).thenReturn(Optional.of(professional));

        assertThatThrownBy(() -> service.getAvailableSlots(professionalId, LocalDate.of(2026, 9, 2), serviceId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("A duração do serviço deve ser maior que zero.");
    }
}
