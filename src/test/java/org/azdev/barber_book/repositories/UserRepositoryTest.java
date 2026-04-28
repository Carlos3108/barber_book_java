package org.azdev.barber_book.repositories;

import org.azdev.barber_book.models.Tenant;
import org.azdev.barber_book.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void findByEmailAndFindAllByTenantIdReturnExpectedUsers() {
        Tenant tenantA = tenantRepository.save(buildTenant("tenant-a"));
        Tenant tenantB = tenantRepository.save(buildTenant("tenant-b"));

        userRepository.save(buildUser("one@test.com", tenantA));
        userRepository.save(buildUser("two@test.com", tenantA));
        userRepository.save(buildUser("other@test.com", tenantB));

        assertThat(userRepository.findByEmail("one@test.com")).isPresent();

        List<User> tenantUsers = userRepository.findAllByTenantId(tenantA.getId());
        assertThat(tenantUsers).hasSize(2);
        assertThat(tenantUsers).extracting(User::getEmail)
                .containsExactlyInAnyOrder("one@test.com", "two@test.com");
    }

    private Tenant buildTenant(String slug) {
        Tenant tenant = new Tenant();
        tenant.setName("Shop " + slug);
        tenant.setSlug(slug);
        tenant.setOwnerEmail(slug + "@test.com");
        tenant.setPlanStatus("TRIAL");
        tenant.setTrialExpiresAt(LocalDateTime.now().plusDays(30));
        return tenant;
    }

    private User buildUser(String email, Tenant tenant) {
        User user = new User();
        user.setName("Owner");
        user.setEmail(email);
        user.setPassword("encoded");
        user.setTenant(tenant);
        return user;
    }
}

