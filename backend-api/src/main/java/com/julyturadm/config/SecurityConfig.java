package com.julyturadm.config;

import com.julyturadm.security.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // `CustomUserDetailsService`: localiza o usuário e suas autoridades.
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            PasswordEncoder passwordEncoder) {

        // `DaoAuthenticationProvider`: valida as credenciais utilizando BCrypt.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    // ---
    // O metodo `AuthenticationManager` será o ponto de entrada para validar as
    // credenciais:
    // `AuthenticationService → AuthenticationManager → DaoAuthenticationProvider →
    // CustomUserDetailsService → UserRepository`
    // O `PasswordEncoder` fará a comparação com o hash BCrypt armazenado no banco.
    // ---
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authenticationProvider(authenticationProvider)

                // ----------------------------------------
                // Requisição Acesso
                // ----------------------------------------
                // POST /api/auth/login Público
                // POST /api/users Protegido
                // GET /api/users Protegido
                // PATCH /api/users/{id}/disable Protegido
                // Demais endpoints Protegido
                // ----------------------------------------
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login")
                        .permitAll()
                        .anyRequest().authenticated())

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(
                                new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(
                                new BearerTokenAccessDeniedHandler()));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter = jwt -> {

            Set<GrantedAuthority> authorities = new LinkedHashSet<>();

            addClaimAuthorities(jwt, "roles", authorities);
            addClaimAuthorities(jwt, "permissions", authorities);

            return authorities;
        };

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return converter;
    }

    private void addClaimAuthorities(
            Jwt jwt,
            String claimName,
            Set<GrantedAuthority> authorities) {

        Object claim = jwt.getClaims().get(claimName);

        if (claim instanceof Collection<?> values) {
            values.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }
    }

}
