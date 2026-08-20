package org.azdev.barber_book.dtos;

public record AuthenticationResponse(String token,
                                     String shopName,
                                     String slug) {
}
