package com.julyturadm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PermissionUpdateRequest(

        @NotBlank(message = "O nome da permissão é obrigatório.")
        @Size(max = 80, message = "O nome da permissão deve ter no máximo 80 caracteres.")
        String name

) {}
