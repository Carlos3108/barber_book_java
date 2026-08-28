package org.azdev.barber_book.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalTime;

public record TenantRequest(@NotBlank(message = "O nome da barbearia não pode ficar em branco.")
                            String name,

                            @NotNull(message = "O horário de abertura é obrigatório.")
                            LocalTime openingTime,

                            @NotNull(message = "O horário de fechamento é obrigatório.")
                            LocalTime closingTime,

                            @NotBlank(message = "O fuso horário é obrigatório.")
                            String timezone,

                            @NotNull(message = "O intervalo de agendamento é obrigatório.")
                            @Min(value = 10, message = "O intervalo mínimo permitido é de 10 minutos.")
                            @Max(value = 60, message = "O intervalo máximo permitido é de 60 minutos.")
                            Integer slotInterval) {
}
