package org.azdev.barber_book.dtos;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentRequest(@NotBlank(message = "O nome do cliente é obrigatório")
                                 String clientName,

                                 @NotBlank(message = "O telefone do cliente é obrigatório")
                                 String clientPhone,

                                 @NotNull(message = "O profissional é obrigatório")
                                 UUID professionalId,

                                 @NotNull(message = "O serviço é obrigatório")
                                 UUID serviceId,

                                 @NotNull(message = "O horário de início é obrigatório")
                                 @FutureOrPresent(message = "O agendamento não pode ser no passado")
                                 OffsetDateTime startTime) {
}
