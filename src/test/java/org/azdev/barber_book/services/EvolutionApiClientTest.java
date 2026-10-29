package org.azdev.barber_book.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvolutionApiClientTest {

    @Test
    void acceptsValidHttpUrl() {
        assertThatCode(() -> new EvolutionApiClient("https://evolution.example/api/", "test-key"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidUrlWithConfigurationMessage() {
        assertThatThrownBy(() -> new EvolutionApiClient("https://", "test-key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("EVOLUTION_API_URL deve ser uma URL HTTP(S) válida");
    }

    @Test
    void rejectsBlankApiKeyWithConfigurationMessage() {
        assertThatThrownBy(() -> new EvolutionApiClient("https://evolution.example", " "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("EVOLUTION_API_KEY não está configurada");
    }
}
