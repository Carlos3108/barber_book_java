package org.azdev.barber_book.finance.events;

import java.math.BigDecimal;
import java.util.UUID;

public record AppointmentCompletedEvent(
        UUID appointmentId,
        UUID tenantId,
        UUID professionalId,
        BigDecimal servicePrice
) {
}
