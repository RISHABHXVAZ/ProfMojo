package com.profmojo.security;

import com.profmojo.controllers.StaffAuthController;
import com.profmojo.integration.BasePostgresContainerTest;
import com.profmojo.models.Staff;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.services.StaffAuthService;
import com.profmojo.testutil.TestDataFactory;
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
@DisplayName("Phase 1 Security - Staff Password Setup Hardening Tests")
class StaffAuthSecurityTest extends BasePostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaffRepository staffRepository;

    @Test
    @DisplayName("HTTP POST to retired /api/staff/auth/set-password returns 404 Not Found and cannot mutate accounts")
    void httpPost_RetiredSetPasswordEndpoint_Returns404_CannotMutateAccount() throws Exception {
        Staff staff = TestDataFactory.createStaff("STAFF_SEC_01", "Security Test Staff", "CSE", "staffsec@profmojo.edu", true, true);
        staff.setPassword(null);
        staffRepository.save(staff);

        mockMvc.perform(post("/api/staff/auth/set-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staffId\":\"STAFF_SEC_01\",\"password\":\"hackedPassword123\"}"))
                .andExpect(status().isNotFound());

        Staff reloaded = staffRepository.findById("STAFF_SEC_01").orElseThrow();
        assertNull(reloaded.getPassword(), "Staff password must remain null after calling retired endpoint");
    }

    @Test
    @DisplayName("Legitimate OTP onboarding routes for staff remain available and functional")
    void legitimateStaffOnboarding_IsAvailable() throws Exception {
        mockMvc.perform(post("/api/onboarding/staff/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"STAFF_NONEXISTENT\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Handler exists; returns 400 (Staff not found / business error), NOT 404
                    assertEquals(400, status);
                });
    }

    @Test
    @DisplayName("Verify unverified set-password endpoint is removed from StaffAuthController")
    void staffAuthController_doesNotContainSetPasswordEndpoint() {
        boolean hasSetPassword = Arrays.stream(StaffAuthController.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("setPassword"));

        assertFalse(hasSetPassword, "StaffAuthController must NOT expose unverified /set-password endpoint");
    }

    @Test
    @DisplayName("Verify StaffAuthService interface does not contain unverified setPassword method")
    void staffAuthService_doesNotContainSetPassword() {
        boolean hasSetPassword = Arrays.stream(StaffAuthService.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals("setPassword"));

        assertFalse(hasSetPassword, "StaffAuthService must NOT expose unverified setPassword method");
    }
}
