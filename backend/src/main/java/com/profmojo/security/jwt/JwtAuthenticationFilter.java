package com.profmojo.security.jwt;

import com.profmojo.models.Professor;
import com.profmojo.repositories.ProfessorRepository;
import com.profmojo.security.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ProfessorRepository professorRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No token provided
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (request.getServletPath().equals("/api/professors/login")
                || request.getServletPath().equals("/api/professors/register")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);
        String profId = jwtUtil.extractProfId(token);

        if (profId != null && jwtUtil.validateToken(token)) {

            Professor prof = professorRepository.findById(profId).orElse(null);

            if (prof != null) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                prof, null,
                                List.of(new SimpleGrantedAuthority(prof.getRole()))
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
