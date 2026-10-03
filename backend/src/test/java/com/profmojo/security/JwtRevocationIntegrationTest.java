package com.profmojo.security;

import com.profmojo.integration.BasePostgresContainerTest;
import com.profmojo.models.Professor;
import com.profmojo.models.Staff;
import com.profmojo.models.Student;
import com.profmojo.repositories.ProfessorRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.repositories.StudentRepository;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.security.jwt.RedisTokenBlacklistService;
import com.profmojo.security.jwt.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("JWT Revocation & Logout Redis Integration Tests")
class JwtRevocationIntegrationTest extends BasePostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private StaffRepository staffRepository;

    @BeforeEach
    void setUp() {
        // Clean test entities if needed
    }

    @Test
    @DisplayName("validToken_NotBlacklisted_AuthenticatesSuccessfully")
    void validToken_NotBlacklisted_AuthenticatesSuccessfully() throws Exception {
        Student student = new Student();
        student.setRegNo("STU_REV_001");
        student.setName("Alice Revoke");
        student.setEmail("alice@test.edu");
        student.setPassword("pass123");
        studentRepository.save(student);

        String token = jwtUtil.generateToken("STU_REV_001", "STUDENT");

        mockMvc.perform(get("/api/students/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regNo").value("STU_REV_001"));
    }

    @Test
    @DisplayName("logout_RevokesToken and subsequent request returns HTTP 401")
    void logout_RevokesToken_SubsequentRequestReturns401() throws Exception {
        Professor prof = new Professor();
        prof.setProfId("PROF_REV_001");
        prof.setName("Dr. Revoke");
        prof.setEmail("revoke@prof.edu");
        prof.setDepartment("CSE");
        prof.setRole("PROFESSOR");
        prof.setPassword("securepass");
        prof.setContactNo("9876543210");
        professorRepository.save(prof);

        String token = jwtUtil.generateToken("PROF_REV_001", "PROFESSOR", "CSE");
        String jti = jwtUtil.extractJti(token);

        // 1. Initial request succeeds
        mockMvc.perform(get("/api/professors/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 2. Perform professor logout
        mockMvc.perform(post("/api/professors/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // 3. Verify Redis blacklist entry exists
        String redisKey = RedisTokenBlacklistService.BLACKLIST_KEY_PREFIX + jti;
        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey)), "Redis must contain blacklist key");

        // 4. Subsequent request with the same token MUST return 401 Unauthorized
        mockMvc.perform(get("/api/professors/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token has been revoked"));
    }

    @Test
    @DisplayName("revocationKeyGetsCorrectTTL: Key in Redis has dynamic TTL matching token remaining lifetime")
    void revocationKeyGetsCorrectTTL() {
        String token = jwtUtil.generateToken("TEST_TTL_USER", "STUDENT");
        String jti = jwtUtil.extractJti(token);

        tokenBlacklistService.revokeToken(token);

        String redisKey = RedisTokenBlacklistService.BLACKLIST_KEY_PREFIX + jti;
        Long expireSeconds = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

        assertNotNull(expireSeconds);
        // Expiration is configured around 1 hour (3600s) in application-test.properties
        assertTrue(expireSeconds > 0 && expireSeconds <= 3600,
                "TTL should match remaining token validity (was " + expireSeconds + "s)");
    }

    @Test
    @DisplayName("blacklistEntryExpiresNaturally: Key naturally evicts after TTL expires")
    void blacklistEntryExpiresNaturally() throws InterruptedException {
        String jti = "test-natural-expire-" + System.currentTimeMillis();
        // Revoke with 1-second TTL
        tokenBlacklistService.revokeJti(jti, 1);

        assertTrue(tokenBlacklistService.isRevoked(jti), "Should be revoked immediately");

        // Wait 1.5 seconds for Redis key to expire naturally
        Thread.sleep(1500);

        assertFalse(tokenBlacklistService.isRevoked(jti), "Should no longer be revoked after TTL natural expiration");
    }

    @Test
    @DisplayName("multiDevice: Revoking Token A leaves Token B valid for the same user")
    void multiDevice_LogoutTokenA_TokenBRemainsValid() throws Exception {
        Professor prof = new Professor();
        prof.setProfId("PROF_MULTI_DEVICE");
        prof.setName("Dr. Multi");
        prof.setEmail("multi@prof.edu");
        prof.setDepartment("CSE");
        prof.setRole("PROFESSOR");
        prof.setPassword("securepass");
        prof.setContactNo("9876543211");
        professorRepository.save(prof);

        // Browser A login
        String tokenA = jwtUtil.generateToken("PROF_MULTI_DEVICE", "PROFESSOR", "CSE");
        // Browser B login
        String tokenB = jwtUtil.generateToken("PROF_MULTI_DEVICE", "PROFESSOR", "CSE");

        assertNotEquals(jwtUtil.extractJti(tokenA), jwtUtil.extractJti(tokenB));

        // Both work initially
        mockMvc.perform(get("/api/professors/me").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/professors/me").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        // Logout Browser A
        mockMvc.perform(post("/api/professors/logout").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Token A is rejected with 401
        mockMvc.perform(get("/api/professors/me").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isUnauthorized());

        // Token B continues to work with 200 OK!
        mockMvc.perform(get("/api/professors/me").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("staffLogout_UpdatesDatabaseAndRedis: Staff offline status and token revocation both succeed")
    void staffLogout_UpdatesDatabaseAndRedis() throws Exception {
        Staff staff = new Staff();
        staff.setStaffId("STAFF_REV_001");
        staff.setName("John Staff");
        staff.setEmail("john@staff.edu");
        staff.setDepartment("CSE");
        staff.setRole("STAFF");
        staff.setAvailable(true);
        staff.setOnline(true);
        staff.setPassword("encodedpass");
        staffRepository.save(staff);

        String staffToken = jwtUtil.generateToken("STAFF_REV_001", "STAFF");
        String jti = jwtUtil.extractJti(staffToken);

        // Perform staff logout
        mockMvc.perform(post("/api/staff/auth/logout")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // 1. Verify PostgreSQL staff.online is false (required for FIFO amenity queue)
        Staff updatedStaff = staffRepository.findById("STAFF_REV_001").orElseThrow();
        assertFalse(updatedStaff.isOnline(), "staff.online must be updated to false in database");

        // 2. Verify Redis token blacklist contains jti
        assertTrue(tokenBlacklistService.isRevoked(jti), "Staff JWT must be blacklisted in Redis");
    }

    @Test
    @DisplayName("adminLogoutRevokesToken: Admin logout successfully revokes admin token")
    void adminLogoutRevokesToken() throws Exception {
        String adminToken = jwtUtil.generateToken("ADMIN_SECRET_KEY_1", "ADMIN", "CSE");
        String jti = jwtUtil.extractJti(adminToken);

        mockMvc.perform(post("/api/admin/auth/logout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        assertTrue(tokenBlacklistService.isRevoked(jti));

        // Subsequent access with revoked admin token is 401
        mockMvc.perform(post("/api/admin/onboarding/add-professor")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("studentLogoutRevokesToken: Student logout successfully revokes student token")
    void studentLogoutRevokesToken() throws Exception {
        Student student = new Student();
        student.setRegNo("STU_REV_002");
        student.setName("Bob Revoke");
        student.setEmail("bob@test.edu");
        student.setPassword("pass123");
        studentRepository.save(student);

        String token = jwtUtil.generateToken("STU_REV_002", "STUDENT");

        mockMvc.perform(post("/api/students/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logoutIsIdempotent: Calling logout multiple times returns 200 OK without errors")
    void logoutIsIdempotent() throws Exception {
        String token = jwtUtil.generateToken("IDEMPOTENT_USER", "STUDENT");

        // First call
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // Second call with same token
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // Third call via role endpoint
        mockMvc.perform(post("/api/students/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
