package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.Catalog;
import org.azdev.barber_book.models.Professional;
import org.azdev.barber_book.models.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProfessionalRepositoryTest {

    @Autowired
    private ProfessionalRepository professionalRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private CatalogRepository catalogRepository;

    @Test
    void findAllActiveByTenantSlugReturnsOnlyActiveProfessionalsFromTenant() {
        Tenant tenantA = tenantRepository.save(buildTenant("tenant-a-public-professionals"));
        Tenant tenantB = tenantRepository.save(buildTenant("tenant-b-public-professionals"));
        Catalog service = catalogRepository.save(buildService("Corte", tenantA));

        professionalRepository.save(buildProfessional("Carlos", true, tenantA, service));
        professionalRepository.save(buildProfessional("Marcos", false, tenantA, service));
        professionalRepository.save(buildProfessional("Ana", true, tenantB, null));

        List<Professional> professionals = professionalRepository.findAllActiveByTenantSlug("tenant-a-public-professionals");

        assertThat(professionals).extracting(Professional::getName)
                .containsExactly("Carlos");
        assertThat(professionals.getFirst().getServices())
                .extracting(Catalog::getName)
                .containsExactly("Corte");
    }

    private Tenant buildTenant(String slug) {
        Tenant tenant = new Tenant();
        tenant.setName("Shop " + slug);
        tenant.setSlug(slug);
        tenant.setPlanStatus("TRIAL");
        tenant.setTrialExpiresAt(OffsetDateTime.now().plusDays(15));
        return tenant;
    }

    private Catalog buildService(String name, Tenant tenant) {
        Catalog service = new Catalog();
        service.setName(name);
        service.setPrice(new BigDecimal("30.00"));
        service.setDurationMinutes(30);
        service.setTenant(tenant);
        return service;
    }

    private Professional buildProfessional(String name, boolean active, Tenant tenant, Catalog service) {
        Professional professional = new Professional();
        professional.setName(name);
        professional.setActive(active);
        professional.setTenant(tenant);
        if (service != null) {
            professional.getServices().add(service);
        }
        return professional;
    }
}
