package org.azdev.barber_book.dtos;

public record RegistrationResponse(String email, boolean verificationRequired) {
}
