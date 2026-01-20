package com.profmojo.repositories;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmenityRequestRepository
        extends JpaRepository<AmenityRequest, Long> {

    List<AmenityRequest> findByProfessorId(String professorId);

    List<AmenityRequest> findByDepartmentAndStatus(
            String department,
            RequestStatus status
    );

    List<AmenityRequest> findByAssignedStaff_StaffIdAndStatus(
            String staffId,
            RequestStatus status
    );

    List<AmenityRequest> findByAssignedStaffAndStatus(
            Staff staff,
            RequestStatus status
    );
    List<AmenityRequest> findByProfessorIdAndStatusOrderByDeliveredAtDesc(
            String professorId,
            RequestStatus status
    );
    List<AmenityRequest> findByProfessorIdAndStatusNot(
            String professorId,
            RequestStatus status
    );




}
