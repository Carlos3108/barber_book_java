package org.azdev.barber_book.dtos;

import java.time.LocalTime;

public record TenantResponse(
        String name,
        LocalTime openingTime,
        LocalTime closingTime,
        String timezone
) {
}