package com.example.userservice.security;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${app.internal.token:}") String internalToken,
            ObjectMapper objectMapper
    ) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS));

        // Protect internal endpoints via a shared secret header.
        http.addFilterBefore(new InternalTokenFilter(internalToken, objectMapper), BearerTokenAuthenticationFilter.class);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/users/health").permitAll()
                .requestMatchers("/api/v1/users/internal/**").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
        );

        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
