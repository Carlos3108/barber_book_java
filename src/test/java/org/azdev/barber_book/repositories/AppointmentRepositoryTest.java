package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.Appointment;
import org.azdev.barber_book.models.Catalog;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.models.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import org.azdev.barber_book.models.enums.AppointmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private CatalogRepository catalogRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private ProfessionalRepository professionalRepository;

    @Test
    void hasOverlappingAppointmentReturnsTrueForConflictingConfirmedSlots() {
        Tenant tenant = saveTenant("tenant-1");
        Professional professional = saveProfessional(tenant, "João");
        Catalog service = saveService(tenant, "Corte");
        OffsetDateTime start = LocalDateTime.of(2026, 4, 28, 10, 0).atOffset(ZoneOffset.UTC);
        OffsetDateTime end = LocalDateTime.of(2026, 4, 28, 11, 0).atOffset(ZoneOffset.UTC);
        saveAppointment(tenant, professional, service, start, end);

        boolean overlapping = appointmentRepository.hasOverlappingAppointment(
                professional.getId(),
                start.plusMinutes(30),
                end.plusMinutes(30),
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED)
        );

        assertThat(overlapping).isTrue();
    }

    @Test
    void findByTenantAndRangeReturnsSortedAppointments() {
        Tenant tenant = saveTenant("tenant-2");
        Professional professional = saveProfessional(tenant, "Maria");
        Catalog service = saveService(tenant, "Barba");
        OffsetDateTime start1 = LocalDateTime.of(2026, 4, 28, 14, 0).atOffset(ZoneOffset.UTC);
        OffsetDateTime end1 = LocalDateTime.of(2026, 4, 28, 15, 0).atOffset(ZoneOffset.UTC);
        OffsetDateTime start2 = LocalDateTime.of(2026, 4, 28, 9, 0).atOffset(ZoneOffset.UTC);
        OffsetDateTime end2 = LocalDateTime.of(2026, 4, 28, 10, 0).atOffset(ZoneOffset.UTC);
        saveAppointment(tenant, professional, service, start1, end1);
        saveAppointment(tenant, professional, service, start2, end2);

        List<Appointment> result = appointmentRepository.findByTenantIdAndStartTimeBetweenOrderByStartTimeAsc(
                tenant.getId(),
                LocalDateTime.of(2026, 4, 28, 0, 0).atOffset(ZoneOffset.UTC),
                LocalDateTime.of(2026, 4, 28, 23, 59).atOffset(ZoneOffset.UTC)
        );

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getStartTime()).isEqualTo(start2);
        assertThat(result.get(1).getStartTime()).isEqualTo(start1);
    }

    private Tenant saveTenant(String slug) {
        Tenant tenant = new Tenant();
        tenant.setName("Shop " + slug);
        tenant.setSlug(slug);
        tenant.setPlanStatus("TRIAL");
        tenant.setTrialExpiresAt(OffsetDateTime.now().plusDays(10));
        return tenantRepository.save(tenant);
    }

    private Catalog saveService(Tenant tenant, String name) {
        Catalog service = new Catalog();
        service.setTenant(tenant);
        service.setName(name);
        service.setPrice(new BigDecimal("25.00"));
        service.setDurationMinutes(30);
        return catalogRepository.save(service);
    }

    private Professional saveProfessional(Tenant tenant, String name) {
        Professional professional = new Professional();
        professional.setTenant(tenant);
        professional.setName(name);
        professional.setActive(true);
        return professionalRepository.save(professional);
    }

    private void saveAppointment(Tenant tenant, Professional professional, Catalog service, OffsetDateTime start, OffsetDateTime end) {
        Appointment appointment = new Appointment();
        appointment.setTenant(tenant);
        appointment.setProfessional(professional);
        appointment.setService(service);
        appointment.setClientName("Cliente");
        appointment.setClientPhone("11999999999");
        appointment.setStartTime(start);
        appointment.setEndTime(end);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appointment);
    }
}

