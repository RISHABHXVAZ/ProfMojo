package com.profmojo.security;

import com.profmojo.exception.SecurityInfrastructureUnavailableException;
import com.profmojo.integration.BasePostgresContainerTest;
import com.profmojo.models.Professor;
import com.profmojo.models.Staff;
import com.profmojo.models.Student;
import com.profmojo.models.dto.SendOtpRequest;
import com.profmojo.ratelimit.RateLimiter;
import com.profmojo.repositories.ProfessorRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.repositories.StudentRepository;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.security.jwt.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 3: Redis Outage Policy & Fail-Closed Security Integration Tests")
class RedisOutageSecurityTest extends BasePostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoSpyBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoSpyBean
    private RateLimiter rateLimiter;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private StaffRepository staffRepository;

    // =========================================================================
    // 1. JWT REVOCATION REDIS OUTAGE TESTS
    // =========================================================================

    @Test
    @DisplayName("Protected endpoint returns HTTP 503 when Redis revocation check fails")
    void protectedEndpoint_WhenRedisRevocationCheckFails_Returns503() throws Exception {
        Student student = new Student();
        student.setRegNo("STU_OUTAGE_001");
        student.setName("Alice Outage");
        student.setEmail("alice.outage@test.edu");
        student.setPassword("pass123");
        studentRepository.save(student);

        String token = jwtUtil.generateToken("STU_OUTAGE_001", "STUDENT");

        // Simulate Redis outage during JTI revocation verification
        doThrow(new SecurityInfrastructureUnavailableException("Security infrastructure unavailable: unable to verify token revocation status"))
                .when(tokenBlacklistService).isRevoked(anyString());

        mockMvc.perform(get("/api/students/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("Authentication service temporarily unavailable"));
    }

    @Test
    @DisplayName("Public health endpoint remains accessible (200 OK) even during Redis outage")
    void publicHealthEndpoint_WhenRedisDown_Returns200() throws Exception {
        // Even if revocation store fails, health endpoint must proceed
        doThrow(new SecurityInfrastructureUnavailableException("Simulated Redis Down"))
                .when(tokenBlacklistService).isRevoked(anyString());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Logout write failure returns explicit HTTP 503 instead of false 200 OK")
    void logout_WhenRedisWriteFails_Returns503Explicitly() throws Exception {
        Staff staff = new Staff();
        staff.setStaffId("STAFF_OUTAGE_001");
        staff.setName("John Outage");
        staff.setEmail("john.outage@staff.edu");
        staff.setDepartment("CSE");
        staff.setRole("STAFF");
        staff.setAvailable(true);
        staff.setOnline(true);
        staff.setPassword("pass123");
        staffRepository.save(staff);

        String token = jwtUtil.generateToken("STAFF_OUTAGE_001", "STAFF");

        // Simulate Redis failure during revoke write
        doThrow(new SecurityInfrastructureUnavailableException("Security infrastructure unavailable: unable to persist token revocation"))
                .when(tokenBlacklistService).revokeToken(anyString());

        mockMvc.perform(post("/api/staff/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("Security infrastructure unavailable: unable to persist token revocation"));
    }

    @Test
    @DisplayName("Admin logout write failure returns explicit HTTP 503 instead of false 200 OK")
    void adminLogout_WhenRedisWriteFails_Returns503Explicitly() throws Exception {
        String token = jwtUtil.generateToken("ADMIN_OUTAGE", "ADMIN", "CSE");
        doThrow(new SecurityInfrastructureUnavailableException("Unable to persist token revocation"))
                .when(tokenBlacklistService).revokeToken(anyString());

        mockMvc.perform(post("/api/admin/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"));
    }

    @Test
    @DisplayName("Student logout write failure returns explicit HTTP 503 instead of false 200 OK")
    void studentLogout_WhenRedisWriteFails_Returns503Explicitly() throws Exception {
        String token = jwtUtil.generateToken("STUDENT_OUTAGE", "STUDENT");
        doThrow(new SecurityInfrastructureUnavailableException("Unable to persist token revocation"))
                .when(tokenBlacklistService).revokeToken(anyString());

        mockMvc.perform(post("/api/students/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"));
    }

    @Test
    @DisplayName("Professor logout write failure returns explicit HTTP 503 instead of false 200 OK")
    void professorLogout_WhenRedisWriteFails_Returns503Explicitly() throws Exception {
        String token = jwtUtil.generateToken("PROF_OUTAGE", "PROFESSOR", "CSE");
        doThrow(new SecurityInfrastructureUnavailableException("Unable to persist token revocation"))
                .when(tokenBlacklistService).revokeToken(anyString());

        mockMvc.perform(post("/api/professors/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"));
    }

    @Test
    @DisplayName("General auth logout write failure returns explicit HTTP 503 instead of false 200 OK")
    void authLogout_WhenRedisWriteFails_Returns503Explicitly() throws Exception {
        String token = jwtUtil.generateToken("GEN_OUTAGE", "PROFESSOR", "CSE");
        doThrow(new SecurityInfrastructureUnavailableException("Unable to persist token revocation"))
                .when(tokenBlacklistService).revokeToken(anyString());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"));
    }

    // =========================================================================
    // 2. RATE LIMITER REDIS OUTAGE TESTS
    // =========================================================================

    @Test
    @DisplayName("Send OTP returns HTTP 503 when Redis rate limiter is unavailable")
    void sendOtp_WhenRedisRateLimiterFails_Returns503() throws Exception {
        doThrow(new SecurityInfrastructureUnavailableException("Security infrastructure unavailable: unable to enforce rate limits"))
                .when(rateLimiter).tryAcquire(anyString(), anyInt(), anyInt());

        SendOtpRequest req = new SendOtpRequest();
        req.setUserId("PROF_OUTAGE_001");

        mockMvc.perform(post("/api/onboarding/professor/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"PROF_OUTAGE_001\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("Security infrastructure unavailable: unable to enforce rate limits"));
    }

    @Test
    @DisplayName("OTP verification attempt check returns HTTP 503 when Redis rate limiter is unavailable")
    void verifyOtp_WhenRedisRateLimiterFails_Returns503() throws Exception {
        doThrow(new SecurityInfrastructureUnavailableException("Security infrastructure unavailable: unable to verify rate limit state"))
                .when(rateLimiter).isLimitExceeded(anyString(), anyInt());

        mockMvc.perform(post("/api/onboarding/professor/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"PROF_OUTAGE_001\",\"otp\":\"123456\",\"newPassword\":\"newPass123\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("Security infrastructure unavailable: unable to verify rate limit state"));
    }

    // =========================================================================
    // 3. RECOVERY TESTS
    // =========================================================================

    @Test
    @DisplayName("Normal operation restored when Redis returns to healthy state")
    void normalOperation_WhenRedisHealthy_Succeeds() throws Exception {
        Professor prof = new Professor();
        prof.setProfId("PROF_RECOVER_001");
        prof.setName("Dr. Recover");
        prof.setEmail("recover@prof.edu");
        prof.setDepartment("CSE");
        prof.setRole("PROFESSOR");
        prof.setPassword("securepass");
        prof.setContactNo("9876543210");
        professorRepository.save(prof);

        String token = jwtUtil.generateToken("PROF_RECOVER_001", "PROFESSOR", "CSE");

        // With healthy Redis (no mocks active), protected request succeeds
        mockMvc.perform(get("/api/professors/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profId").value("PROF_RECOVER_001"));
    }
}
