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
            String username = jwtUtil.extractUsername(token);
            String role = jwtUtil.extractRole(token);

            // 🔥 SET FOR CONTROLLER USE
            request.setAttribute("username", username);

            // 🔥 STUDENT AUTH
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

            // 🔥 PROFESSOR AUTH
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
                    System.out.println("DEBUG: Staff Authenticated: " + username); // Add this
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    staff,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_STAFF"))
                            );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    System.out.println("DEBUG: Staff NOT found in DB for username: " + username); // Add this
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
            // token expired → let request fail naturally
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