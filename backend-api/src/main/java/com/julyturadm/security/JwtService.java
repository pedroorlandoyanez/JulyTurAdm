package com.julyturadm.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

//---
//A classe JwtService.java gera tokens JWT assinados com HS256, incluindo:
//- Identificação do usuário (sub).
//- Data de emissão (iat).
//- Expiração em 30 minutos (exp).
//- Papéis funcionais (roles).
//- Permissões granulares (permissions).
//---
// `JwtEncoder`: assina os tokens com HS256.
// `JwtDecoder`: valida a assinatura com HS256.
// `JWT_SECRET`: é carregado por variável de ambiente, sem expor a chave no código.
// O `application.yml`: está apontando corretamente para `${JWT_SECRET}`.
//---
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final long TOKEN_EXPIRATION_MINUTES = 30;

    private final JwtEncoder jwtEncoder;

    public String generateToken(UserDetails userDetails) {

        Instant issuedAt = Instant.now();

        Instant expiresAt = issuedAt.plus(
                TOKEN_EXPIRATION_MINUTES,
                ChronoUnit.MINUTES
        );

        List<String> authorities = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .distinct()
                .sorted()
                .toList();

        List<String> roles = authorities.stream()
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();

        List<String> permissions = authorities.stream()
                .filter(authority -> !authority.startsWith("ROLE_"))
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userDetails.getUsername())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }
}
