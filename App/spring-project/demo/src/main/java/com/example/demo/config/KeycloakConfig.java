package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class KeycloakConfig {

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll());
//                requestMatchers("/**").authenticated().
//                requestMatchers("/health").permitAll());
        return http.build();
    }
}
