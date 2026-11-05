package com.julyturadm.service;

import com.julyturadm.dto.request.LoginRequest;
import com.julyturadm.dto.response.LoginResponse;
import com.julyturadm.security.JwtService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void shouldAuthenticateUserAndReturnJwtToken() {

        LoginRequest request = new LoginRequest(
                "usuario@teste.com",
                "Senha123"
        );

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("usuario@teste.com")
                .password("senha-criptografada")
                .authorities("ROLE_ADMIN", "USER_CREATE")
                .build();

        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(userDetails);

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(jwtService.generateToken(userDetails))
                .thenReturn("jwt-token-teste");

        LoginResponse response = authenticationService.authenticate(request);

        assertNotNull(response);
        assertEquals("jwt-token-teste", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(1800L, response.expiresIn());

        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class));

        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void shouldThrowExceptionWhenCredentialsAreInvalid() {

        LoginRequest request = new LoginRequest(
                "usuario@teste.com",
                "SenhaIncorreta"
        );

        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciais inválidas."));

        assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.authenticate(request)
        );

        verify(authenticationManager).authenticate(
                any(UsernamePasswordAuthenticationToken.class));

        verifyNoInteractions(jwtService);
    }
}
