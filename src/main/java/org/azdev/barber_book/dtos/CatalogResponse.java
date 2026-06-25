package org.azdev.barber_book.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogResponse(UUID id,
                              String name,
                              BigDecimal price,
                              Integer durationMinutes,
                              boolean active) {
}
