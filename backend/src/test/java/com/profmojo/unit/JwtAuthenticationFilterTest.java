package com.profmojo.unit;

import com.profmojo.models.Student;
import com.profmojo.repositories.*;
import com.profmojo.security.jwt.JwtAuthenticationFilter;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.security.jwt.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("JwtAuthenticationFilter Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private ProfessorRepository professorRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private DepartmentSecretRepository departmentSecretRepository;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(
                jwtUtil,
                professorRepository,
                studentRepository,
                adminRepository,
                staffRepository,
                departmentSecretRepository,
                tokenBlacklistService
        );
    }

    @Test
    @DisplayName("validToken_NotBlacklisted_AuthenticatesSuccessfully")
    void validToken_NotBlacklisted_AuthenticatesSuccessfully() throws Exception {
        String token = "valid.jwt.token";
        String jti = "valid-jti-123";
        String username = "STU001";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/students/me");

        Claims claims = Jwts.claims();
        claims.setSubject(username);
        claims.put("role", "STUDENT");
        claims.setId(jti);

        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(tokenBlacklistService.isRevoked(jti)).thenReturn(false);

        Student student = new Student();
        student.setRegNo(username);
        when(studentRepository.findById(username)).thenReturn(Optional.of(student));

        filter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("ROLE_STUDENT", SecurityContextHolder.getContext().getAuthentication().getAuthorities().iterator().next().getAuthority());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("revokedToken_Returns401 and does not query user repository")
    void revokedToken_Returns401_DoesNotQueryUserRepository() throws Exception {
        String token = "revoked.jwt.token";
        String jti = "blacklisted-jti-456";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/students/me");

        Claims claims = Jwts.claims();
        claims.setSubject("STU001");
        claims.put("role", "STUDENT");
        claims.setId(jti);

        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(tokenBlacklistService.isRevoked(jti)).thenReturn(true);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // CRITICAL: verify repositories were NEVER contacted
        verifyNoInteractions(studentRepository);
        verifyNoInteractions(professorRepository);
        verifyNoInteractions(staffRepository);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("malformedToken_FailsBeforeBlacklistLookup")
    void malformedToken_FailsBeforeBlacklistLookup() throws Exception {
        String token = "malformed.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractAllClaims(token)).thenThrow(new MalformedJwtException("Invalid token"));

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(tokenBlacklistService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("expiredToken_FailsBeforeBlacklistLookup")
    void expiredToken_FailsBeforeBlacklistLookup() throws Exception {
        String token = "expired.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractAllClaims(token)).thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(tokenBlacklistService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("tokenWithoutJti_Returns401")
    void tokenWithoutJti_Returns401() throws Exception {
        String token = "legacy.jwt.token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        Claims claims = Jwts.claims();
        claims.setSubject("USER_NO_JTI");
        claims.put("role", "STUDENT");
        // No JTI set

        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verifyNoInteractions(tokenBlacklistService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("logoutIsIdempotent: Revoked token calling /logout is allowed through")
    void logoutWithRevokedToken_AllowedThroughForIdempotency() throws Exception {
        String token = "revoked.jwt.token";
        String jti = "already-revoked-jti";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getRequestURI()).thenReturn("/api/auth/logout");

        Claims claims = Jwts.claims();
        claims.setSubject("STU001");
        claims.put("role", "STUDENT");
        claims.setId(jti);

        when(jwtUtil.extractAllClaims(token)).thenReturn(claims);
        when(tokenBlacklistService.isRevoked(jti)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        // Does NOT return 401; passes through to logout handler
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain).doFilter(request, response);
    }
}
