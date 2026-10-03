package com.profmojo.security.jwt;

import com.profmojo.models.*;
import com.profmojo.repositories.*;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ProfessorRepository professorRepository;
    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final StaffRepository staffRepository;
    private final DepartmentSecretRepository departmentSecretRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // 1. Signature and expiration validation happen first
            io.jsonwebtoken.Claims claims = jwtUtil.extractAllClaims(token);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            String jti = claims.getId();

            // 2. Reject legacy tokens without JTI
            if (jti == null || jti.isBlank()) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid token: missing JTI claim\"}");
                return;
            }

            // 3. Check Redis token blacklist
            if (tokenBlacklistService.isRevoked(jti)) {
                String requestUri = request.getRequestURI();
                boolean isLogoutRequest = requestUri != null && requestUri.endsWith("/logout");
                if (!isLogoutRequest) {
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token has been revoked\"}");
                    return;
                }
            }

            // 4. Role resolution and DB lookups (only for valid, non-revoked tokens)
            request.setAttribute("username", username);

            if ("STUDENT".equals(role)) {
                Student student = studentRepository.findById(username).orElse(null);

                if (student != null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    student,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                            );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }

            if ("PROFESSOR".equals(role)) {
                Professor prof = professorRepository.findById(username).orElse(null);

                if (prof != null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    prof,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR"))
                            );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }

            if ("ROLE_STAFF".equals(role) || "STAFF".equals(role)) {
                Staff staff = staffRepository.findById(username).orElse(null);

                if (staff != null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    staff,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
                            );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }

            if ("ADMIN".equals(role)) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        } catch (ExpiredJwtException e) {
            // token expired -> let request fail naturally without querying Redis
        }
        catch (io.jsonwebtoken.security.SignatureException |
               io.jsonwebtoken.MalformedJwtException |
               io.jsonwebtoken.UnsupportedJwtException |
               IllegalArgumentException e) {

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}