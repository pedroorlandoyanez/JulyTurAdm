package com.julyturadm.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

// O objetivo será configurar dois componentes:
//- JwtEncoder: assina os tokens emitidos pela aplicação.
//- JwtDecoder: valida os tokens recebidos pela API.
@Configuration
public class JwtConfig {

    @Value("${julyturadm.security.jwt.secret}")
    private String jwtSecret;

    // jwtSecretKey()	Decodifica a chave Base64
    @Bean
    public SecretKey jwtSecretKey() {

        byte[] decodedKey = Base64.getDecoder().decode(jwtSecret);

        return new SecretKeySpec(
            decodedKey,
            "HmacSHA256"
        );
    }

    // jwtEncoder()	Assina os tokens com HS256
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {

        return new NimbusJwtEncoder(
            new ImmutableSecret<>(jwtSecretKey)
        );
    }

    // jwtDecoder()	Valida a assinatura dos tokens com HS256
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {

        return NimbusJwtDecoder
            .withSecretKey(jwtSecretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }
}
