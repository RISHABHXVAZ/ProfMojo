package com.profmojo.security;

import com.profmojo.controllers.StudentController;
import com.profmojo.integration.BasePostgresContainerTest;
import com.profmojo.repositories.StudentRepository;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.services.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 1 Security - Student Registration Hardening Tests")
class StudentAuthSecurityTest extends BasePostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("HTTP POST to retired /api/students/register is rejected and cannot create student")
    void httpPost_RetiredRegisterEndpoint_Rejected_CannotCreateStudent() throws Exception {
        long initialCount = studentRepository.count();

        // 1. Unauthenticated request to /api/students/register is rejected by security chain (403 Forbidden)
        mockMvc.perform(post("/api/students/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regNo\":\"STU_MALICIOUS\",\"name\":\"Malicious\",\"email\":\"mal@test.edu\",\"password\":\"pass\"}"))
                .andExpect(status().isForbidden());

        // 2. Even with an authenticated student token, dispatcher servlet returns 404 Not Found (no handler exists)
        com.profmojo.models.Student existingStudent = com.profmojo.models.Student.builder()
                .regNo("STU_EXISTING")
                .name("Existing Student")
                .email("existing@test.edu")
                .password("hash")
                .role("STUDENT")
                .build();
        studentRepository.save(existingStudent);
        long countAfterExisting = studentRepository.count();

        String studentToken = jwtUtil.generateToken("STU_EXISTING", "STUDENT", "CSE");
        mockMvc.perform(post("/api/students/register")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"regNo\":\"STU_MALICIOUS\",\"name\":\"Malicious\",\"email\":\"mal@test.edu\",\"password\":\"pass\"}"))
                .andExpect(status().isNotFound());

        // Verify database was NOT mutated by malicious payload
        assertEquals(countAfterExisting, studentRepository.count(), "No student should be registered via retired endpoint");
        assertFalse(studentRepository.existsById("STU_MALICIOUS"));
    }

    @Test
    @DisplayName("Legitimate OTP onboarding routes for students remain available and functional")
    void legitimateStudentOnboarding_IsAvailable() throws Exception {
        mockMvc.perform(post("/api/onboarding/student/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"STU_NONEXISTENT\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Handler exists; returns 400 (Registration number not found in master records), NOT 404
                    assertEquals(400, status);
                });
    }

    @Test
    @DisplayName("Verify unverified register endpoint is removed from StudentController")
    void studentController_doesNotContainRegisterEndpoint() {
        boolean hasRegister = Arrays.stream(StudentController.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("register"));

        assertFalse(hasRegister, "StudentController must NOT expose unverified /register endpoint");
    }

    @Test
    @DisplayName("Verify StudentService interface does not contain unverified register method")
    void studentService_doesNotContainRegister() {
        boolean hasRegister = Arrays.stream(StudentService.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("register"));

        assertFalse(hasRegister, "StudentService must NOT expose unverified register method");
    }
}
