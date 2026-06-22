package org.azdev.barber_book.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentResponse (UUID id,
                                   String clientName,
                                   String clientPhone,
                                   OffsetDateTime startTime,
                                   OffsetDateTime endTime,
                                   String status,
                                   String professionalName,
                                   String serviceName,
                                   BigDecimal servicePrice)
{}
