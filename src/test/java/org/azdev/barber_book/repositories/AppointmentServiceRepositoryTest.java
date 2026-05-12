package org.azdev.barber_book.repositories;

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
class AppointmentServiceRepositoryTest {

    @Autowired
    private AppointmentServiceRepository appointmentServiceRepository;
    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void findAllByTenantIdReturnsOnlyTenantServices() {
        Tenant tenantA = tenantRepository.save(buildTenant("tenant-a-services"));
        Tenant tenantB = tenantRepository.save(buildTenant("tenant-b-services"));

        appointmentServiceRepository.save(buildService("Corte", tenantA));
        appointmentServiceRepository.save(buildService("Barba", tenantA));
        appointmentServiceRepository.save(buildService("Progressiva", tenantB));

        List<AppointmentService> services = appointmentServiceRepository.findAllByTenantIdAndActiveTrue(tenantA.getId());

        assertThat(services).hasSize(2);
        assertThat(services).extracting(AppointmentService::getName)
                .containsExactlyInAnyOrder("Corte", "Barba");
    }

    private Tenant buildTenant(String slug) {
        Tenant tenant = new Tenant();
        tenant.setName("Shop " + slug);
        tenant.setSlug(slug);
        tenant.setPlanStatus("TRIAL");
        tenant.setTrialExpiresAt(LocalDateTime.now().plusDays(15));
        return tenant;
    }

    private AppointmentService buildService(String name, Tenant tenant) {
        AppointmentService service = new AppointmentService();
        service.setName(name);
        service.setPrice(new BigDecimal("30.00"));
        service.setDurationMinutes(30);
        service.setTenant(tenant);
        return service;
    }
}

