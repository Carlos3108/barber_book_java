package org.azdev.barber_book.repositories;

import java.time.OffsetDateTime;

public interface ClientFrequencyProjection {
    String getClientName();
    String getClientPhone();
    Long getCompletedAppointmentsThisMonth();
    OffsetDateTime getLastCompletedAt();
}
