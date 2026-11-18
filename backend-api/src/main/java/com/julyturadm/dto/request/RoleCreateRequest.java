package com.julyturadm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleCreateRequest(

        @NotBlank(message = "O nome do perfil é obrigatório.")
        @Size(max = 50, message = "O nome do perfil deve ter no máximo 50 caracteres.")
        String name

) {
}