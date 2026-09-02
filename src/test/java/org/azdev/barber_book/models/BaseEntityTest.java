package org.azdev.barber_book.models;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    @Test
    void baseEntitySetsAuditFieldsOnCreation() {
        TestEntity entity = new TestEntity();

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    private static class TestEntity extends BaseEntity {
    }
}

