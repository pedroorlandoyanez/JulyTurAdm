package com.julyturadm.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@TestConfiguration
public class TestDatabaseSafetyConfiguration {

    @Bean
    static BeanFactoryPostProcessor databaseSafetyGuard(Environment environment) {

        return beanFactory -> {
            String url = environment.getProperty(
                "spring.datasource.url",
                ""
            );

            boolean isTestDatabase = url.matches(
                "jdbc:mysql://[^/]+/bd_paytur_test(?:\\?.*)?"
            );

            if (!isTestDatabase) {
                throw new IllegalStateException(
                    "TESTE INTERROMPIDO: o banco configurado não é bd_paytur_test. "
                    + "URL encontrada: " + url
                );
            }
        };
    }
}