package org.azdev.barber_book.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

public record ProfessionalRequest(
        @NotBlank(message = "O nome do profissional é obrigatório")
        String name,

        @NotNull(message = "A lista de serviços é obrigatória")
        Set<UUID> serviceIds) {

}
