package com.profmojo.unit;

import com.profmojo.models.*;
import com.profmojo.models.dto.VerifyOtpSetPasswordRequest;
import com.profmojo.repositories.*;
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
    private OnboardingOtpRepository otpRepository;

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
        OnboardingOtp validOtp = TestDataFactory.createOtp("PROF101", "PROFESSOR", "123456", 5);

        when(otpRepository.findById("PROF101")).thenReturn(Optional.of(validOtp));
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

        verify(otpRepository).deleteById("PROF101");
    }

    @Test
    @DisplayName("verifyProfessorOtpAndSetPassword: Wrong role in OTP throws exception")
    void verifyProfessorOtpAndSetPassword_WrongRole_ThrowsException() {
        OnboardingOtp studentOtp = TestDataFactory.createOtp("PROF101", "STUDENT", "123456", 5);
        when(otpRepository.findById("PROF101")).thenReturn(Optional.of(studentOtp));

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

        verify(otpRepository).save(argThat(otp ->
                otp.getUserId().equals("STU202") &&
                otp.getRole().equals("STUDENT") &&
                otp.getOtp().length() == 6
        ));

        verify(emailService).send(eq("grace@hopper.edu"), contains("ProfMojo OTP"), anyString());
    }
}
