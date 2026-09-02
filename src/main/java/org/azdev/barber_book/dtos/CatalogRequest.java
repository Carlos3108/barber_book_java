package org.azdev.barber_book.dtos;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record CatalogRequest(
        @NotBlank(message = "O nome do serviço é obrigatório.")
        String name,

        @NotNull(message = "O preço do serviço é obrigatório.")
        @DecimalMin(value = "0.00", inclusive = true, message = "O preço não pode ser negativo.")
        @Digits(integer = 8, fraction = 2, message = "O preço deve ter no máximo 8 dígitos inteiros e 2 casas decimais.")
        BigDecimal price,

        @NotNull(message = "A duração é obrigatória")
        @Min(value = 5, message = "A duração mínima é de 5 minutos.")
        Integer durationMinutes
) {
}
