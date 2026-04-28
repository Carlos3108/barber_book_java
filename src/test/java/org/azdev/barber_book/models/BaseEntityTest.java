package org.azdev.barber_book.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Test
    void preUpdateRefreshesUpdatedAt() {
        TestEntity entity = new TestEntity();
        LocalDateTime before = LocalDateTime.now().minusMinutes(5);
        entity.setUpdatedAt(before);

        entity.preUpdate();

        assertThat(entity.getUpdatedAt()).isAfter(before);
    }

    private static class TestEntity extends BaseEntity {
    }
}

