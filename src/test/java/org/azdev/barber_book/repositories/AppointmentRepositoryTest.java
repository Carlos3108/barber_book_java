package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.Appointment;
import org.azdev.barber_book.models.AppointmentService;
import org.azdev.barber_book.models.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private AppointmentServiceRepository appointmentServiceRepository;
    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void hasOverlappingAppointmentReturnsTrueForConflictingConfirmedSlots() {
        Tenant tenant = saveTenant("tenant-1");
        AppointmentService service = saveService(tenant, "Corte");
        saveAppointment(tenant, service, LocalDateTime.of(2026, 4, 28, 10, 0), LocalDateTime.of(2026, 4, 28, 11, 0), "CONFIRMED");

        boolean overlapping = appointmentRepository.hasOverlappingAppointment(
                tenant.getId(),
                LocalDateTime.of(2026, 4, 28, 10, 30),
                LocalDateTime.of(2026, 4, 28, 11, 30)
        );

        assertThat(overlapping).isTrue();
    }

    @Test
    void findByTenantAndRangeReturnsSortedAppointments() {
        Tenant tenant = saveTenant("tenant-2");
        AppointmentService service = saveService(tenant, "Barba");
        saveAppointment(tenant, service, LocalDateTime.of(2026, 4, 28, 14, 0), LocalDateTime.of(2026, 4, 28, 15, 0), "CONFIRMED");
        saveAppointment(tenant, service, LocalDateTime.of(2026, 4, 28, 9, 0), LocalDateTime.of(2026, 4, 28, 10, 0), "CONFIRMED");

        List<Appointment> result = appointmentRepository.findByTenantIdAndStartTimeBetweenOrderByStartTimeAsc(
                tenant.getId(),
                LocalDateTime.of(2026, 4, 28, 0, 0),
                LocalDateTime.of(2026, 4, 28, 23, 59)
        );

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().getStartTime()).isEqualTo(LocalDateTime.of(2026, 4, 28, 9, 0));
        assertThat(result.get(1).getStartTime()).isEqualTo(LocalDateTime.of(2026, 4, 28, 14, 0));
    }

    private Tenant saveTenant(String slug) {
        Tenant tenant = new Tenant();
        tenant.setName("Shop " + slug);
        tenant.setSlug(slug);
        tenant.setOwnerEmail(slug + "@test.com");
        tenant.setPlanStatus("TRIAL");
        tenant.setTrialExpiresAt(LocalDateTime.now().plusDays(10));
        return tenantRepository.save(tenant);
    }

    private AppointmentService saveService(Tenant tenant, String name) {
        AppointmentService service = new AppointmentService();
        service.setTenant(tenant);
        service.setName(name);
        service.setPrice(new BigDecimal("25.00"));
        service.setDurationMinutes(30);
        return appointmentServiceRepository.save(service);
    }

    private void saveAppointment(Tenant tenant, AppointmentService service, LocalDateTime start, LocalDateTime end, String status) {
        Appointment appointment = new Appointment();
        appointment.setTenant(tenant);
        appointment.setService(service);
        appointment.setClientName("Cliente");
        appointment.setClientPhone("11999999999");
        appointment.setStartTime(start);
        appointment.setEndTime(end);
        appointment.setStatus(status);
        appointmentRepository.save(appointment);
    }
}

