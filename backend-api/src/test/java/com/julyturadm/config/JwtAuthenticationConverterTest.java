package com.julyturadm.config;

import com.julyturadm.security.CustomUserDetailsService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class JwtAuthenticationConverterTest {

    private JwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        SecurityConfig securityConfig =
                new SecurityConfig(mock(CustomUserDetailsService.class));

        converter = securityConfig.jwtAuthenticationConverter();
    }

    private Jwt createJwt(Object roles, Object permissions) {
        return Jwt.withTokenValue("token-teste")
                .header("alg", "HS256")
                .claim("roles", roles)
                .claim("permissions", permissions)
                .build();
    }

    private Set<String> getAuthorities(Jwt jwt) {
        return converter.convert(jwt)
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    @Test
    void shouldConvertRolesAndPermissionsIntoAuthorities() {

        Jwt jwt = createJwt(
                List.of("ROLE_ADMIN"),
                List.of("USER_CREATE", "USER_UPDATE")
        );

        Set<String> authorities = getAuthorities(jwt);

        assertEquals(3, authorities.size());
        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("USER_CREATE"));
        assertTrue(authorities.contains("USER_UPDATE"));
    }

    @Test
    void shouldRemoveDuplicateAuthorities() {

        Jwt jwt = createJwt(
                List.of("ROLE_ADMIN", "ROLE_ADMIN"),
                List.of("USER_CREATE", "USER_CREATE")
        );

        Set<String> authorities = getAuthorities(jwt);

        assertEquals(2, authorities.size());
        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertTrue(authorities.contains("USER_CREATE"));
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenClaimsAreMissing() {

        Jwt jwt = createJwt(null, null);

        Set<String> authorities = getAuthorities(jwt);

        assertTrue(authorities.isEmpty());
    }

    @Test
    void shouldIgnoreClaimsThatAreNotCollectionsOfStrings() {

        Jwt jwt = createJwt(
                "ROLE_ADMIN",
                List.of("USER_CREATE", 123, true)
        );

        Set<String> authorities = getAuthorities(jwt);

        assertEquals(1, authorities.size());
        assertTrue(authorities.contains("USER_CREATE"));
    }
}