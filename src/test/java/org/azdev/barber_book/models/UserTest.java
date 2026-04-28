package org.azdev.barber_book.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void userDetailsContractMatchesCurrentRules() {
        Tenant tenant = new Tenant();
        tenant.setPlanStatus("TRIAL");

        User user = new User();
        user.setEmail("owner@test.com");
        user.setPassword("secret");
        user.setTenant(tenant);

        assertThat(user.getUsername()).isEqualTo("owner@test.com");
        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();

        tenant.setPlanStatus("SUSPENDED");
        assertThat(user.isAccountNonLocked()).isFalse();
    }
}

