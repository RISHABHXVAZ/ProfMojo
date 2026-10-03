package com.profmojo.repositories;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AmenityRequestRepository
        extends JpaRepository<AmenityRequest, Long> {

    List<AmenityRequest> findByProfessorId(String professorId);

    @EntityGraph(attributePaths = {"assignedStaff"})
    List<AmenityRequest> findByDepartmentAndStatus(
            String department,
            RequestStatus status
    );

    @EntityGraph(attributePaths = {"assignedStaff"})
    List<AmenityRequest> findByAssignedStaff_StaffIdAndStatus(
            String staffId,
            RequestStatus status
    );

    @EntityGraph(attributePaths = {"assignedStaff"})
    List<AmenityRequest> findByAssignedStaffAndStatus(
            Staff staff,
            RequestStatus status
    );

    @EntityGraph(attributePaths = {"assignedStaff"})
    List<AmenityRequest> findByProfessorIdAndStatusOrderByDeliveredAtDesc(
            String professorId,
            RequestStatus status
    );

    @EntityGraph(attributePaths = {"assignedStaff"})
    List<AmenityRequest> findByProfessorIdAndStatusNot(
            String professorId,
            RequestStatus status
    );

    @Query("SELECT ar FROM AmenityRequest ar WHERE ar.status = :status AND ar.assignmentDeadline < :now")
    List<AmenityRequest> findRequestsWithBreachedAssignmentSLA(
            @Param("status") RequestStatus status,
            @Param("now") LocalDateTime now
    );

    @EntityGraph(attributePaths = {"assignedStaff"})
    List<AmenityRequest> findByDepartmentAndStatusOrderByCreatedAtAsc(
            String department, RequestStatus status);

    Optional<AmenityRequest> findFirstByDepartmentAndStatusOrderByCreatedAtAsc(
            String department, RequestStatus status);


    Optional<AmenityRequest> findFirstByStatusAndDepartmentOrderByCreatedAtAsc(
            RequestStatus status,
            String department
    );

    List<AmenityRequest> findByStatus(RequestStatus requestStatus);

    long countByStatus(RequestStatus status);

    /**
     * Atomically claims the oldest QUEUED request for a given department.
     *
     * <p>Uses PostgreSQL {@code FOR UPDATE SKIP LOCKED}: the first transaction
     * that executes this will lock the selected row. Any concurrent transaction
     * will skip that locked row and either lock the next-oldest one or return
     * empty. This eliminates the read-then-write race in FIFO staff assignment
     * without requiring an external lock manager.
     *
     * @param department the department to search in
     * @param status     the status string (pass {@code "QUEUED"})
     * @return the oldest unlocked QUEUED request, if one exists
     */
    @Query(
        value = """
            SELECT * FROM amenity_request
             WHERE status = :status
               AND department = :department
             ORDER BY created_at ASC
             LIMIT 1
             FOR UPDATE SKIP LOCKED
            """,
        nativeQuery = true
    )
    Optional<AmenityRequest> findOldestQueuedForUpdate(
            @Param("department") String department,
            @Param("status") String status
    );

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("UPDATE AmenityRequest ar SET ar.slaBreached = true, ar.deliverySlaBreached = true WHERE ar.status = :status AND ar.slaDeadline < :now AND ar.slaBreached = false")
    int markBreachedDeliverySlaRequests(
            @Param("status") RequestStatus status,
            @Param("now") LocalDateTime now
    );
}
