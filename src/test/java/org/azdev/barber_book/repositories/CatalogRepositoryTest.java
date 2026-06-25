package org.azdev.barber_book.repositories;

import org.azdev.barber_book.dtos.CatalogResponse;
import org.azdev.barber_book.models.Catalog;
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
class CatalogRepositoryTest {

    @Autowired
    private CatalogRepository catalogRepository;
    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void findAllByTenantIdReturnsOnlyTenantServices() {
        Tenant tenantA = tenantRepository.save(buildTenant("tenant-a-services"));
        Tenant tenantB = tenantRepository.save(buildTenant("tenant-b-services"));

        catalogRepository.save(buildService("Corte", tenantA));
        catalogRepository.save(buildService("Barba", tenantA));
        catalogRepository.save(buildService("Progressiva", tenantB));

        List<Catalog> services = catalogRepository.findAllByTenantIdAndActiveTrue(tenantA.getId());

        assertThat(services).hasSize(2);
        assertThat(services).extracting(Catalog::getName)
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

    private Catalog buildService(String name, Tenant tenant) {
        Catalog service = new Catalog();
        service.setName(name);
        service.setPrice(new BigDecimal("30.00"));
        service.setDurationMinutes(30);
        service.setTenant(tenant);
        return service;
    }
}

