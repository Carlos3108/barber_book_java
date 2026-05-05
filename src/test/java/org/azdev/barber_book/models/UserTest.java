package org.azdev.barber_book.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void userEntityStoresCoreFields() {
        Tenant tenant = new Tenant();
        tenant.setPlanStatus("TRIAL");
        tenant.setName("Barbearia Central");

        User user = new User();
        user.setName("Carlos");
        user.setEmail("owner@test.com");
        user.setPassword("secret");
        user.setTenant(tenant);

        assertThat(user.getName()).isEqualTo("Carlos");
        assertThat(user.getEmail()).isEqualTo("owner@test.com");
        assertThat(user.getPassword()).isEqualTo("secret");
        assertThat(user.getTenant()).isSameAs(tenant);
        assertThat(user.getTenant().getPlanStatus()).isEqualTo("TRIAL");
    }
}

