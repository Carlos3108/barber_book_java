package org.azdev.barber_book.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "O nome da barbearia é obrigatório")
        String shopName,

        @NotBlank(message = "O nome do proprietário é obrigatório")
        String ownerName,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        String password,

        @AssertTrue(message = "É necessário confirmar que você tem 18 anos ou mais")
        boolean adultConfirmed,

        @AssertTrue(message = "É necessário aceitar os Termos de Uso e a Política de Privacidade")
        boolean termsAccepted
) {
}