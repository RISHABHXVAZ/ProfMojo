package com.profmojo.unit;

import com.profmojo.models.Professor;
import com.profmojo.models.ProfessorMaster;
import com.profmojo.models.StudentMaster;
import com.profmojo.models.dto.VerifyOtpSetPasswordRequest;
import com.profmojo.otp.OtpSource;
import com.profmojo.otp.OtpStore;
import com.profmojo.otp.StoredOtp;
import com.profmojo.ratelimit.OtpRateLimiter;
import com.profmojo.repositories.ProfessorMasterRepository;
import com.profmojo.repositories.ProfessorRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.repositories.StudentMasterRepository;
import com.profmojo.repositories.StudentRepository;
import com.profmojo.services.EmailService;
import com.profmojo.services.impl.OnboardingAuthServiceImpl;
import com.profmojo.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OnboardingAuthService Unit Tests")
class OnboardingAuthServiceTest {

    @Mock
    private ProfessorMasterRepository professorMasterRepository;

    @Mock
    private ProfessorRepository professorRepository;

    @Mock
    private OtpStore otpStore;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private StudentMasterRepository studentMasterRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private OtpRateLimiter otpRateLimiter;

    @Mock
    private com.profmojo.metrics.AppMetricsService appMetricsService;

    @InjectMocks
    private OnboardingAuthServiceImpl onboardingAuthService;

    private ProfessorMaster profMaster;
    private StudentMaster studentMaster;

    @BeforeEach
    void setUp() {
        profMaster = TestDataFactory.createProfessorMaster("PROF101", "Dr. Alan Turing", "CSE", "alan@turing.edu");
        studentMaster = TestDataFactory.createStudentMaster("STU202", "Grace Hopper", "CSE", "grace@hopper.edu");
    }

    @Test
    @DisplayName("verifyProfessorOtpAndSetPassword: Valid OTP saves Professor with encoded password and deletes OTP")
    void verifyProfessorOtpAndSetPassword_Success() {
        StoredOtp validOtp = new StoredOtp("123456", "PROFESSOR", "PROF101", OtpSource.REDIS, LocalDateTime.now().plusMinutes(5));

        when(otpStore.findOtp("PROFESSOR", "PROF101")).thenReturn(Optional.of(validOtp));
        when(professorMasterRepository.findById("PROF101")).thenReturn(Optional.of(profMaster));
        when(passwordEncoder.encode("StrongPassword@123")).thenReturn("encoded-bcrypt-hash");

        VerifyOtpSetPasswordRequest request = new VerifyOtpSetPasswordRequest();
        request.setUserId("PROF101");
        request.setOtp("123456");
        request.setPassword("StrongPassword@123");

        onboardingAuthService.verifyProfessorOtpAndSetPassword(request);

        ArgumentCaptor<Professor> profCaptor = ArgumentCaptor.forClass(Professor.class);
        verify(professorRepository).save(profCaptor.capture());
        Professor savedProf = profCaptor.getValue();
        assertEquals("PROF101", savedProf.getProfId());
        assertEquals("encoded-bcrypt-hash", savedProf.getPassword());
        assertEquals("PROFESSOR", savedProf.getRole());

        verify(otpStore).deleteOtp("PROFESSOR", "PROF101");
        verify(otpRateLimiter).resetVerifyAttempts("PROFESSOR", "PROF101");
    }

    @Test
    @DisplayName("verifyProfessorOtpAndSetPassword: Wrong role in OTP throws exception")
    void verifyProfessorOtpAndSetPassword_WrongRole_ThrowsException() {
        StoredOtp studentOtp = new StoredOtp("123456", "STUDENT", "PROF101", OtpSource.REDIS, LocalDateTime.now().plusMinutes(5));
        when(otpStore.findOtp("PROFESSOR", "PROF101")).thenReturn(Optional.of(studentOtp));

        VerifyOtpSetPasswordRequest request = new VerifyOtpSetPasswordRequest();
        request.setUserId("PROF101");
        request.setOtp("123456");
        request.setPassword("pass");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> onboardingAuthService.verifyProfessorOtpAndSetPassword(request));
        assertEquals("Invalid OTP role", ex.getMessage());
        verify(professorRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendStudentOtp: Valid student ID generates OTP and dispatches email")
    void sendStudentOtp_Success() {
        when(studentMasterRepository.findById("STU202")).thenReturn(Optional.of(studentMaster));

        onboardingAuthService.sendStudentOtp("STU202");

        verify(otpStore).saveOtp(eq("STUDENT"), eq("STU202"), argThat(otp -> otp != null && otp.length() == 6), eq(Duration.ofSeconds(300)));
        verify(otpRateLimiter).resetVerifyAttempts("STUDENT", "STU202");
        verify(emailService).send(eq("grace@hopper.edu"), contains("ProfMojo OTP"), anyString());
    }
}
