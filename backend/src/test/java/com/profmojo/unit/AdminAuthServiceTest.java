package com.profmojo.unit;

import com.profmojo.models.DepartmentSecret;
import com.profmojo.models.dto.AdminLoginResponse;
import com.profmojo.models.dto.AdminVerifyOtpRequest;
import com.profmojo.otp.OtpSource;
import com.profmojo.otp.OtpStore;
import com.profmojo.otp.StoredOtp;
import com.profmojo.ratelimit.OtpRateLimiter;
import com.profmojo.repositories.DepartmentSecretRepository;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.services.EmailService;
import com.profmojo.services.impl.AdminAuthServiceImpl;
import com.profmojo.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuthService Unit Tests")
class AdminAuthServiceTest {

    @Mock
    private DepartmentSecretRepository secretRepository;

    @Mock
    private OtpStore otpStore;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private OtpRateLimiter otpRateLimiter;

    @Mock
    private com.profmojo.security.jwt.TokenBlacklistService tokenBlacklistService;

    @Mock
    private com.profmojo.metrics.AppMetricsService appMetricsService;

    @InjectMocks
    private AdminAuthServiceImpl adminAuthService;

    private DepartmentSecret activeSecret;
    private final String testKey = "test-secret-123";

    @BeforeEach
    void setUp() {
        activeSecret = TestDataFactory.createDepartmentSecret(testKey, "CSE", "admin@test.edu", true);
    }

    @Test
    @DisplayName("sendOtp: Valid secret key generates 6-digit OTP, saves it, and dispatches email")
    void sendOtp_ValidSecretKey_GeneratesAndDispatchesOtp() {
        when(secretRepository.findById(testKey)).thenReturn(Optional.of(activeSecret));

        adminAuthService.sendOtp(testKey);

        verify(otpRateLimiter).resetVerifyAttempts("ADMIN", testKey);

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(otpStore).saveOtp(eq("ADMIN"), eq(testKey), otpCaptor.capture(), eq(Duration.ofSeconds(300)));
        String savedOtp = otpCaptor.getValue();
        assertNotNull(savedOtp);
        assertEquals(6, savedOtp.length());

        verify(emailService).send(eq("admin@test.edu"), contains("ProfMojo Admin OTP"), contains(savedOtp));
    }

    @Test
    @DisplayName("sendOtp: Non-existent secret key throws RuntimeException")
    void sendOtp_NonExistentSecretKey_ThrowsException() {
        when(secretRepository.findById("unknown-key")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminAuthService.sendOtp("unknown-key"));
        assertEquals("Invalid secret key", ex.getMessage());
        verifyNoInteractions(emailService);
        verify(otpStore, never()).saveOtp(any(), any(), any(), any());
    }

    @Test
    @DisplayName("sendOtp: Disabled secret key throws RuntimeException and prevents email dispatch")
    void sendOtp_DisabledSecretKey_ThrowsException() {
        DepartmentSecret disabledSecret = TestDataFactory.createDepartmentSecret(testKey, "CSE", "admin@test.edu", false);
        when(secretRepository.findById(testKey)).thenReturn(Optional.of(disabledSecret));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminAuthService.sendOtp(testKey));
        assertEquals("Secret key disabled", ex.getMessage());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("verifyOtpAndLogin: Successful verification returns JWT token and deletes OTP")
    void verifyOtpAndLogin_ValidOtp_ReturnsTokenAndDeletesOtp() {
        String validOtpCode = "654321";
        StoredOtp validOtp = new StoredOtp(validOtpCode, "ADMIN", testKey, OtpSource.REDIS, LocalDateTime.now().plusMinutes(5));

        when(otpStore.findOtp("ADMIN", testKey)).thenReturn(Optional.of(validOtp));
        when(secretRepository.findById(testKey)).thenReturn(Optional.of(activeSecret));
        when(jwtUtil.generateToken(testKey, "ADMIN", "CSE")).thenReturn("mock-jwt-token-12345");

        AdminVerifyOtpRequest request = new AdminVerifyOtpRequest();
        request.setSecretKey(testKey);
        request.setOtp(validOtpCode);

        AdminLoginResponse response = adminAuthService.verifyOtpAndLogin(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token-12345", response.getToken());
        assertEquals("CSE", response.getDepartment());
        assertEquals("ADMIN", response.getRole());

        // Verify OTP is deleted to prevent replay attacks
        verify(otpStore).deleteOtp("ADMIN", testKey);
        verify(otpRateLimiter).resetVerifyAttempts("ADMIN", testKey);
    }

    @Test
    @DisplayName("verifyOtpAndLogin: Non-existent OTP throws RuntimeException")
    void verifyOtpAndLogin_OtpNotFound_ThrowsException() {
        when(otpStore.findOtp("ADMIN", testKey)).thenReturn(Optional.empty());

        AdminVerifyOtpRequest request = new AdminVerifyOtpRequest();
        request.setSecretKey(testKey);
        request.setOtp("123456");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminAuthService.verifyOtpAndLogin(request));
        assertEquals("OTP not found", ex.getMessage());
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    @DisplayName("verifyOtpAndLogin: Expired OTP throws RuntimeException")
    void verifyOtpAndLogin_ExpiredOtp_ThrowsException() {
        StoredOtp expiredOtp = new StoredOtp("123456", "ADMIN", testKey, OtpSource.REDIS, LocalDateTime.now().minusMinutes(1));

        when(otpStore.findOtp("ADMIN", testKey)).thenReturn(Optional.of(expiredOtp));

        AdminVerifyOtpRequest request = new AdminVerifyOtpRequest();
        request.setSecretKey(testKey);
        request.setOtp("123456");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminAuthService.verifyOtpAndLogin(request));
        assertEquals("OTP expired", ex.getMessage());
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }

    @Test
    @DisplayName("verifyOtpAndLogin: Incorrect OTP code throws RuntimeException")
    void verifyOtpAndLogin_IncorrectOtp_ThrowsException() {
        StoredOtp validOtp = new StoredOtp("999999", "ADMIN", testKey, OtpSource.REDIS, LocalDateTime.now().plusMinutes(5));
        when(otpStore.findOtp("ADMIN", testKey)).thenReturn(Optional.of(validOtp));

        AdminVerifyOtpRequest request = new AdminVerifyOtpRequest();
        request.setSecretKey(testKey);
        request.setOtp("111111");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminAuthService.verifyOtpAndLogin(request));
        assertEquals("Invalid OTP", ex.getMessage());
        verify(otpRateLimiter).recordFailedVerifyAttempt("ADMIN", testKey);
        verify(jwtUtil, never()).generateToken(any(), any(), any());
    }
}
