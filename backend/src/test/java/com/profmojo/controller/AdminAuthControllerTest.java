package com.profmojo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profmojo.controllers.AdminAuthController;
import com.profmojo.models.dto.AdminLoginResponse;
import com.profmojo.models.dto.AdminSendOtpRequest;
import com.profmojo.models.dto.AdminVerifyOtpRequest;
import com.profmojo.security.jwt.JwtAuthenticationFilter;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.services.AdminAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import com.profmojo.exception.GlobalExceptionHandler;

@WebMvcTest(AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("AdminAuthController MockMvc Tests")
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AdminAuthService adminAuthService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /api/admin/auth/send-otp: Returns 200 and success message when key is valid")
    void sendOtp_Success_Returns200() throws Exception {
        AdminSendOtpRequest req = new AdminSendOtpRequest();
        req.setSecretKey("19472026");

        doNothing().when(adminAuthService).sendOtp("19472026");

        mockMvc.perform(post("/api/admin/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent to admin email"));
    }

    @Test
    @DisplayName("POST /api/admin/auth/send-otp: Returns 400 when service throws exception")
    void sendOtp_InvalidKey_Returns400() throws Exception {
        AdminSendOtpRequest req = new AdminSendOtpRequest();
        req.setSecretKey("wrong-key");

        doThrow(new RuntimeException("Invalid secret key")).when(adminAuthService).sendOtp("wrong-key");

        mockMvc.perform(post("/api/admin/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid secret key"));
    }

    @Test
    @DisplayName("POST /api/admin/auth/verify-otp: Returns 200 with JWT token and role when OTP is valid")
    void verifyOtp_Success_Returns200WithToken() throws Exception {
        AdminVerifyOtpRequest req = new AdminVerifyOtpRequest();
        req.setSecretKey("19472026");
        req.setOtp("123456");

        AdminLoginResponse loginResponse = new AdminLoginResponse("mock-jwt-token", "CSE", "ADMIN");
        when(adminAuthService.verifyOtpAndLogin(any(AdminVerifyOtpRequest.class))).thenReturn(loginResponse);

        mockMvc.perform(post("/api/admin/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.department").value("CSE"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
