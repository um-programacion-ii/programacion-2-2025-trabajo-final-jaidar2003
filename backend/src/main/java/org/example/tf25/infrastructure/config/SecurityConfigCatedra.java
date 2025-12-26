package org.example.tf25.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("catedra")
public class SecurityConfigCatedra {

    @Bean
    public SecurityFilterChain securityFilterChainCatedra(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );
        // Importante: NO habilitar oauth2ResourceServer en este perfil
        return http.build();
    }
}
