package org.azdev.barber_book.dtos;

import java.util.UUID;

public record ProfessionalResponse(UUID id,
                                   String name,
                                   boolean active) {
}
