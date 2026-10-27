package org.azdev.barber_book.notifications.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentCreatedEvent(UUID appointmentId,
                                      UUID tenantId,
                                      String clientName,
                                      String clientPhone,
                                      OffsetDateTime startTime) {
}
