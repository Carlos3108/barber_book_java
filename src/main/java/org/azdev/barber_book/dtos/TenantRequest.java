package org.azdev.barber_book.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record TenantRequest(@NotBlank(message = "O nome da barbearia não pode ficar em branco.")
                            String name,

                            @NotNull(message = "O horário de abertura é obrigatório.")
                            LocalTime openingTime,

                            @NotNull(message = "O horário de fechamento é obrigatório.")
                            LocalTime closingTime,

                            @NotBlank(message = "O fuso horário é obrigatório.")
                            String timezone) {
}
