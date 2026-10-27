package org.azdev.barber_book.dtos;

public record WhatsAppStatusResponse(
        boolean connected,
        String instanceName,
        boolean isReminderEnabled,
        boolean isDailySummaryEnabled
) {
}
