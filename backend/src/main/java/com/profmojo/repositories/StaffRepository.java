package com.profmojo.repositories;

import com.profmojo.models.Staff;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StaffRepository
        extends JpaRepository<Staff, String> {

    List<Staff> findByDepartmentAndAvailableTrue(String department);
    List<Staff> findByDepartment(String department);
    List<Staff> findByAvailableTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Staff s WHERE s.staffId = :staffId")
    Optional<Staff> findByIdForUpdate(@Param("staffId") String staffId);
}

