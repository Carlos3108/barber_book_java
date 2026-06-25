package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.Appointment;
import org.azdev.barber_book.models.Catalog;
import org.azdev.barber_book.models.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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

    @Test
    void hasOverlappingAppointmentReturnsTrueForConflictingConfirmedSlots() {
        Tenant tenant = saveTenant("tenant-1");
        Catalog service = saveService(tenant, "Corte");
        OffsetDateTime start = LocalDateTime.of(2026, 4, 28, 10, 0).atOffset(ZoneOffset.UTC);
        OffsetDateTime end = LocalDateTime.of(2026, 4, 28, 11, 0).atOffset(ZoneOffset.UTC);
        saveAppointment(tenant, service, start, end);

        boolean overlapping = appointmentRepository.hasOverlappingAppointment(
                tenant.getId(),
                start.plusMinutes(30),
                end.plusMinutes(30)
        );

        assertThat(overlapping).isTrue();
    }

    @Test
    void findByTenantAndRangeReturnsSortedAppointments() {
        Tenant tenant = saveTenant("tenant-2");
        Catalog service = saveService(tenant, "Barba");
        OffsetDateTime start1 = LocalDateTime.of(2026, 4, 28, 14, 0).atOffset(ZoneOffset.UTC);
        OffsetDateTime end1 = LocalDateTime.of(2026, 4, 28, 15, 0).atOffset(ZoneOffset.UTC);
        OffsetDateTime start2 = LocalDateTime.of(2026, 4, 28, 9, 0).atOffset(ZoneOffset.UTC);
        OffsetDateTime end2 = LocalDateTime.of(2026, 4, 28, 10, 0).atOffset(ZoneOffset.UTC);
        saveAppointment(tenant, service, start1, end1);
        saveAppointment(tenant, service, start2, end2);

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
        tenant.setTrialExpiresAt(LocalDateTime.now().plusDays(10));
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

    private void saveAppointment(Tenant tenant, Catalog service, OffsetDateTime start, OffsetDateTime end) {
        Appointment appointment = new Appointment();
        appointment.setTenant(tenant);
        appointment.setService(service);
        appointment.setClientName("Cliente");
        appointment.setClientPhone("11999999999");
        appointment.setStartTime(start);
        appointment.setEndTime(end);
        appointment.setStatus("CONFIRMED");
        appointmentRepository.save(appointment);
    }
}

