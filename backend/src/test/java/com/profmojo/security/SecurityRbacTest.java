package com.profmojo.security;

import com.profmojo.integration.BasePostgresContainerTest;
import com.profmojo.security.jwt.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Security & Role-Based Access Control (RBAC) Tests")
class SecurityRbacTest extends BasePostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("Public endpoint is accessible without authentication")
    void publicEndpoint_PermitAll_NotForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"secretKey\":\"non-existent-test-key\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Status should be 400 (Bad Request from business validation), NOT 401 Unauthorized or 403 Forbidden
                    org.junit.jupiter.api.Assertions.assertNotEquals(401, status, "Public route should not return 401");
                    org.junit.jupiter.api.Assertions.assertNotEquals(403, status, "Public route should not return 403");
                });
    }

    @Test
    @DisplayName("Admin protected endpoint rejects unauthenticated request")
    void adminEndpoint_Unauthenticated_Rejected() throws Exception {
        mockMvc.perform(post("/api/admin/onboarding/add-professor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin protected endpoint rejects Student role with 403 Forbidden")
    void adminEndpoint_StudentRole_Forbidden() throws Exception {
        String studentToken = jwtUtil.generateToken("STUDENT_01", "STUDENT", "CSE");

        mockMvc.perform(post("/api/admin/onboarding/add-professor")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Professor protected endpoint rejects Student role with 403 Forbidden")
    void professorEndpoint_StudentRole_Forbidden() throws Exception {
        String studentToken = jwtUtil.generateToken("STUDENT_01", "STUDENT", "CSE");

        mockMvc.perform(get("/api/professor/classes")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Protected endpoint with malformed/expired token returns 403 Forbidden")
    void protectedEndpoint_MalformedToken_Forbidden() throws Exception {
        mockMvc.perform(post("/api/admin/onboarding/add-professor")
                        .header("Authorization", "Bearer invalid-garbage-token-string")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
