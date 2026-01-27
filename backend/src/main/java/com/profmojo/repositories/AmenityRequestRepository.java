package com.profmojo.repositories;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @Query("SELECT ar FROM AmenityRequest ar WHERE ar.status = :status AND ar.assignmentDeadline < :now")
    List<AmenityRequest> findRequestsWithBreachedAssignmentSLA(
            @Param("status") RequestStatus status,
            @Param("now") LocalDateTime now
    );

    List<AmenityRequest> findByDepartmentAndStatusOrderByCreatedAtAsc(
            String department, RequestStatus status);

    Optional<AmenityRequest> findFirstByDepartmentAndStatusOrderByCreatedAtAsc(
            String department, RequestStatus status);


    Optional<AmenityRequest>
    findFirstByStatusAndDepartmentOrderByCreatedAtAsc(
            RequestStatus status,
            String department
    );
}
