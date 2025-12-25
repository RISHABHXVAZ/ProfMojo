package com.profmojo.config;

import com.profmojo.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth

                        // 🔓 PUBLIC
                        .requestMatchers(
                                "/api/students/login",
                                "/api/students/register",
                                "/api/professors/login",
                                "/api/professors/register",
                                "/api/canteen/login",
                                "/api/canteen/register",
                                "/api/canteen/check-id/**"
                        ).permitAll()

                        // 👀 PROFESSOR CAN VIEW CANTEENS
                        .requestMatchers("/api/canteen/active")
                        .hasAnyRole("PROFESSOR", "STUDENT")

                        // 🏪 ONLY CANTEEN STAFF
                        .requestMatchers("/api/canteen/**")
                        .hasRole("CANTEEN")

                        // OTHER PROTECTED
                        .requestMatchers("/api/students/**").authenticated()
                        .requestMatchers("/api/professors/**").authenticated()
                        .requestMatchers("/api/attendance/student/**").hasRole("STUDENT")
                        .requestMatchers("/api/attendance/**").hasRole("PROFESSOR")
                        .requestMatchers("/api/canteen/menu/my").hasRole("CANTEEN")
                        .requestMatchers("/api/canteen/menu/add").hasRole("CANTEEN")
                        .requestMatchers("/api/canteen/menu/canteen/**").hasRole("PROFESSOR")
                        .requestMatchers("/api/orders/**").authenticated()

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
