package org.azdev.barber_book.exception;

public class EmailNotVerifiedException extends RuntimeException {
    public static final String CODE = "EMAIL_NOT_VERIFIED";

    public EmailNotVerifiedException() {
        super("Confirme seu e-mail para acessar a conta.");
    }
}
