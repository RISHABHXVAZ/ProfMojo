package com.profmojo.services;

import com.profmojo.models.dto.VerifyOtpSetPasswordRequest;

public interface OnboardingAuthService {

    void sendProfessorOtp(String profId);

    void verifyProfessorOtpAndSetPassword(VerifyOtpSetPasswordRequest request);

    void sendStudentOtp(String regNo);

    void verifyStudentOtpAndSetPassword(VerifyOtpSetPasswordRequest request);

    void sendStaffOtp(String staffId);

    void verifyStaffOtpAndSetPassword(VerifyOtpSetPasswordRequest request);


}
