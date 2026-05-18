package org.azdev.barber_book.dtos;

import jakarta.validation.constraints.NotBlank;

public record ProfessionalRequest(
        @NotBlank(message = "O nome do profissional é obrigatório")
        String name) {
}
