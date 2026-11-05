package com.julyturadm.dto.response;

public record LoginResponse(

    String accessToken,

    String tokenType,
    // O campo `expiresIn` representará o tempo de validade do token em segundos: 1800 segundos (30 minutos).
    long expiresIn

) {
}
