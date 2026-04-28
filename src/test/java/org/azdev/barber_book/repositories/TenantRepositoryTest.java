package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TenantRepositoryTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void findBySlugAndExistsByOwnerEmailWorkAsExpected() {
        Tenant tenant = new Tenant();
        tenant.setName("Barber X");
        tenant.setSlug("barber-x");
        tenant.setOwnerEmail("owner@barberx.com");
        tenant.setTrialExpiresAt(LocalDateTime.now().plusDays(1));
        tenant.setPlanStatus("TRIAL");
        tenantRepository.save(tenant);

        assertThat(tenantRepository.findBySlug("barber-x")).isPresent();
        assertThat(tenantRepository.existsByOwnerEmail("owner@barberx.com")).isTrue();
    }

    @Test
    void findExpiredTenantsReturnsOnlyTrialOrActiveExpiredOnes() {
        tenantRepository.save(buildTenant("trial-expired", "TRIAL", LocalDateTime.now().minusDays(1)));
        tenantRepository.save(buildTenant("active-expired", "ACTIVE", LocalDateTime.now().minusDays(1)));
        tenantRepository.save(buildTenant("active-not-expired", "ACTIVE", LocalDateTime.now().plusDays(3)));
        tenantRepository.save(buildTenant("suspended-expired", "SUSPENDED", LocalDateTime.now().minusDays(1)));

        List<Tenant> expired = tenantRepository.findExpiredTenants(LocalDateTime.now());

        assertThat(expired).extracting(Tenant::getSlug)
                .containsExactlyInAnyOrder("trial-expired", "active-expired");
    }

    private Tenant buildTenant(String slug, String status, LocalDateTime expiresAt) {
        Tenant tenant = new Tenant();
        tenant.setName("Shop " + slug);
        tenant.setSlug(slug);
        tenant.setOwnerEmail(slug + "@test.com");
        tenant.setPlanStatus(status);
        tenant.setTrialExpiresAt(expiresAt);
        return tenant;
    }
}

