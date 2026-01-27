package com.profmojo.config;

import com.profmojo.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 🔓 PUBLIC
                        .requestMatchers(
                                "/api/students/login",
                                "/api/students/register",
                                "/api/students/check-id/**",
                                "/api/professors/login",
                                "/api/professors/register",
                                "/api/professors/check-id/**",
                                "/api/admin/auth/**",
                                "/api/staff/auth/**",
                                "/ws-notifications/**"
                        ).permitAll()

                                // 🔔 NOTIFICATIONS
// Admin history needs ADMIN role
                                .requestMatchers("/api/notifications/admin/**").hasRole("ADMIN")

// Staff history needs STAFF role
                                .requestMatchers("/api/notifications/staff/**").hasRole("STAFF")

// General notifications (if any) - authenticated users
                                .requestMatchers("/api/notifications/**").authenticated()
                                .requestMatchers(
                                        "/api/admin/amenities/*/queue"
                                ).hasRole("ADMIN")

                        // 🛠️ ADMIN — MUST COME FIRST
                                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                                // 👨‍🏫 PROFESSOR AMENITIES
                        .requestMatchers("/api/amenities/**").hasRole("PROFESSOR")

                        // 👷 STAFF
                        .requestMatchers("/api/staff/**").hasRole("STAFF")

                        // OTHER PROTECTED
                        .requestMatchers("/api/students/**").authenticated()
                        .requestMatchers("/api/professors/**").authenticated()

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