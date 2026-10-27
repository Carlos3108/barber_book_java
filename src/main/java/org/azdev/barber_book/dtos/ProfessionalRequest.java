package org.azdev.barber_book.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Set;
import java.util.UUID;

public record ProfessionalRequest(
        @NotBlank(message = "O nome do profissional é obrigatório")
        String name,

        @NotNull(message = "A lista de serviços é obrigatória")
        Set<UUID> serviceIds,

        @NotBlank(message = "O telefone é obrigatório")
        @Pattern(regexp = "^\\d{10,11}$", message = "O telefone deve conter apenas de 10 a 11 números")
        String phone) {

}
