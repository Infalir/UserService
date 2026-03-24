package com.appname.userservice.config;

import com.appname.userservice.security.JwtAuthenticationFilter;
import com.appname.userservice.security.SecurityExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityExceptionHandler securityExceptionHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()

                        .requestMatchers(HttpMethod.POST,   "/api/v1/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/api/v1/users/{id}/activate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/api/v1/users/{id}/deactivate").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET,    "/api/v1/users/{id}").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/users/{id}").hasAnyRole("ADMIN", "USER")

                        .requestMatchers(HttpMethod.GET,    "/api/v1/cards").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST,   "/api/v1/users/{userId}/cards").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/users/{userId}/cards").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET,    "/api/v1/cards/{id}").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.PUT,    "/api/v1/cards/{id}").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.PATCH,  "/api/v1/cards/{id}/activate").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.PATCH,  "/api/v1/cards/{id}/deactivate").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/cards/{id}").hasAnyRole("ADMIN", "USER")

                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}