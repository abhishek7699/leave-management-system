package com.lms.leave.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/leaves").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.GET, "/api/leaves/my").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.PUT, "/api/leaves/*/cancel").hasRole("EMPLOYEE")
                .requestMatchers(HttpMethod.GET, "/api/leaves/pending").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/leaves/*/approve").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/leaves/*/reject").hasRole("MANAGER")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
