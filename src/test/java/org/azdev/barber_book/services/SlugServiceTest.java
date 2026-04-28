package org.azdev.barber_book.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugServiceTest {

    private final SlugService slugService = new SlugService();

    @Test
    void generateSlugNormalizesTextAndAddsSuffix() {
        String slug = slugService.generateSlug("Barbearia do Zé !!!");

        assertThat(slug).startsWith("barbearia-do-ze");
        assertThat(slug).matches("barbearia-do-ze-*[a-f0-9]{4}");
    }
}


