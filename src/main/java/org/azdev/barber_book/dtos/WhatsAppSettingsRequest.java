package org.azdev.barber_book.dtos;

public record WhatsAppSettingsRequest(
        boolean isReminderEnabled,
        boolean isDailySummaryEnabled
) {
}
