package com.pwos.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Who may do what. Rules are checked top to bottom; the first match wins.
 * ADMIN: everything. CLERK: items. SUPERVISOR: orders. VIEWER: read only.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/error", "/actuator/health", "/actuator/prometheus").permitAll()
                .requestMatchers("/users", "/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,
                        "/items/*/delete", "/orders/*/delete").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,
                        "/items", "/items/*").hasAnyRole("ADMIN", "CLERK")
                .requestMatchers(HttpMethod.GET,
                        "/items/*/edit").hasAnyRole("ADMIN", "CLERK")
                .requestMatchers(HttpMethod.POST,
                        "/orders", "/orders/*", "/orders/*/status",
                        "/orders/*/cancel").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers(HttpMethod.GET,
                        "/orders/*/edit").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers("/history").hasAnyRole("ADMIN", "VIEWER")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
