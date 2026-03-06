package org.azdev.barber_book.dtos;

public record RegisterRequest(String shopName, String ownerName, String email, String password) {
}
