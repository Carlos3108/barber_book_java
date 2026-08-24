package org.azdev.barber_book.dtos;

import java.time.OffsetDateTime;

public record ApiErrorResponse(OffsetDateTime timestamp,
                               Integer status,
                               String error,
                               String message,
                               String path) {
}
