package com.julyturadm.service;

import com.julyturadm.dto.request.LoginRequest;
import com.julyturadm.dto.response.LoginResponse;
import com.julyturadm.security.JwtService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

//---
// A classe `AuthenticationService.java`
// O fluxo será:
//
//1. Receber o e-mail e a senha.
//2. Encaminhar as credenciais ao `AuthenticationManager`.
//3. O Spring Security validará a senha usando BCrypt.
//4. Se a autenticação for aprovada, o `JwtService` emitirá o token.
//5. Retornar o token no `LoginResponse`.
//---
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final long TOKEN_EXPIRATION_SECONDS = 1800;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse authenticate(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.email(),
                                request.password()
                        )
                );

        String accessToken = jwtService.generateToken(
                (org.springframework.security.core.userdetails.UserDetails)
                        authentication.getPrincipal()
        );

        return new LoginResponse(
                accessToken,
                "Bearer",
                TOKEN_EXPIRATION_SECONDS
        );
    }
}
