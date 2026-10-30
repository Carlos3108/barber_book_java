package org.azdev.barber_book.dtos;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AgeConfirmationValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void registerRequiresAdultConfirmation() {
        var invalid = new RegisterRequest("Barbearia", "Carlos", "c@test.com", "12345678", false, true);
        var valid = new RegisterRequest("Barbearia", "Carlos", "c@test.com", "12345678", true, true);

        assertThat(validator.validate(invalid)).hasSize(1);
        assertThat(validator.validate(valid)).isEmpty();
    }

    @Test
    void registerRequiresTermsAcceptance() {
        var invalid = new RegisterRequest("Barbearia", "Carlos", "c@test.com", "12345678", true, false);

        assertThat(validator.validate(invalid)).hasSize(1);
    }

    @Test
    void publicAppointmentRequiresAdultOrGuardianConfirmation() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        var invalid = new PublicAppointmentRequest("Ana", "11999999999", UUID.randomUUID(), UUID.randomUUID(), start, false);
        var valid = new PublicAppointmentRequest("Ana", "11999999999", UUID.randomUUID(), UUID.randomUUID(), start, true);

        assertThat(validator.validate(invalid)).hasSize(1);
        assertThat(validator.validate(valid)).isEmpty();
    }
}
