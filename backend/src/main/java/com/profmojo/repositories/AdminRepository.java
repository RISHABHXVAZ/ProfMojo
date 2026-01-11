package com.profmojo.repositories;

import com.profmojo.models.Admin;
import com.profmojo.models.AmenityRequest;
import com.profmojo.models.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, String> {
    Optional<Admin> findByAdminId(String adminId);
    Optional<Admin> findByEmail(String email);
    boolean existsByEmail(String email);
}

