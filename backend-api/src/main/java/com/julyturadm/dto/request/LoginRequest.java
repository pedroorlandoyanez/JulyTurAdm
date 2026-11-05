package com.julyturadm.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Informe um endereço de e-mail válido.")
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres.")
    String email,

    @NotBlank(message = "A senha é obrigatória.")
    @Size(max = 72, message = "A senha excede o tamanho permitido.")
    String password

) {
}
