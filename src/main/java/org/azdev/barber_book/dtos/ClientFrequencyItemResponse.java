package org.azdev.barber_book.dtos;

public record ClientFrequencyItemResponse(
        String clientName,
        String clientPhone,
        long completedAppointmentsThisMonth,
        long daysSinceLastCompleted
) {
}
