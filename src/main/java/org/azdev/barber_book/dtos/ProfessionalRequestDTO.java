package org.azdev.barber_book.dtos;

import jakarta.validation.constraints.NotBlank;

public record ProfessionalRequestDTO(
        @NotBlank(message = "O nome do profissional é obrigatório")
        String name) {
}
