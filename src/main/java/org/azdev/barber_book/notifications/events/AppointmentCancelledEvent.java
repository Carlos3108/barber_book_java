package org.azdev.barber_book.notifications.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentCancelledEvent(UUID appointmentId,
                                        UUID tenantId,
                                        String clientName,
                                        String clientPhone,
                                        String professionalName,
                                        String professionalPhone,
                                        OffsetDateTime startTime) {
}
