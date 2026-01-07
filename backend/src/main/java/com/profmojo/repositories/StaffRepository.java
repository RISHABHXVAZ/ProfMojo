package com.profmojo.repositories;

import com.profmojo.models.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffRepository
        extends JpaRepository<Staff, Long> {

    List<Staff> findByDepartmentAndAvailableTrue(String department);
}

