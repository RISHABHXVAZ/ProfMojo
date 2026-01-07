package com.profmojo.repositories;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmenityRequestRepository
        extends JpaRepository<AmenityRequest, Long> {

    List<AmenityRequest> findByDepartmentAndStatus(
            String department,
            RequestStatus status
    );

    List<AmenityRequest> findByProfessorId(String professorId);
}

