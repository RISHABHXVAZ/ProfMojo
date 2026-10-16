package com.profmojo.repositories;

import com.profmojo.models.OnboardingOtp;
import com.profmojo.models.OnboardingOtpId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingOtpRepository extends JpaRepository<OnboardingOtp, OnboardingOtpId> {

    Optional<OnboardingOtp> findByRoleIgnoreCaseAndUserId(String role, String userId);

    void deleteByRoleIgnoreCaseAndUserId(String role, String userId);

    boolean existsByRoleIgnoreCaseAndUserId(String role, String userId);
}
