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
                        // OPTIONS requests always allowed
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 🔓 PUBLIC ENDPOINTS (No authentication needed)
                        .requestMatchers(
                                "/api/students/login",
                                "/api/students/register",
                                "/api/students/check-id/**",
                                "/api/professors/login",
                                "/api/professors/register",
                                "/api/professors/check-id/**",
                                "/api/admin/auth/**",
                                "/api/staff/auth/**",
                                "/api/onboarding/**"
                        ).permitAll()

                        // 🛠️ ADMIN ENDPOINTS (Highest priority - most specific)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 👨‍🏫 PROFESSOR ENDPOINTS
                        .requestMatchers("/api/amenities/**").hasRole("PROFESSOR")
                        .requestMatchers("/api/professors/**").hasRole("PROFESSOR")

                        // 👷 STAFF ENDPOINTS
                        .requestMatchers("/api/staff/**").hasRole("STAFF")

                        // 👨‍🎓 STUDENT ENDPOINTS
                        .requestMatchers("/api/students/**").hasRole("STUDENT")

                        // 🔔 NOTIFICATION ENDPOINTS (Role-based)
                        .requestMatchers("/api/notifications/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/notifications/staff/**").hasRole("STAFF")
                        .requestMatchers("/api/notifications/professor/**").hasRole("PROFESSOR")
                        .requestMatchers("/api/notifications/**").authenticated()

                        // 📚 ATTENDANCE & NOTICE ENDPOINTS (Professor only)
                        .requestMatchers("/api/attendance/student/**").hasRole("STUDENT")
                        .requestMatchers("/api/attendance/**").hasRole("PROFESSOR")

                                // STUDENT notice endpoints
                                .requestMatchers("/api/notices/student/**").hasRole("STUDENT")

                                // PROFESSOR notice endpoints
                                .requestMatchers("/api/notices/**").hasRole("PROFESSOR")


                                // Default - all other endpoints need authentication
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