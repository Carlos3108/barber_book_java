package org.azdev.barber_book.dtos;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CatalogRequest(
        @NotBlank(message = "O nome do serviço é obrigatório.")
        String name,

        @NotNull(message = "O preço do serviço é obrigatório.")
        @Min(value = 0, message = "O preço não pode ser negativo.")
        BigDecimal price,

        @NotNull(message = "A duração é obrigatória")
        @Min(value = 5, message = "A duração mínima é de 5 minutos.")
        Integer durationMinutes
) {
}
