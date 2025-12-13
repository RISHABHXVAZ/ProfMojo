package com.profmojo.repositories;

import com.profmojo.models.StudentMaster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentMasterRepository extends JpaRepository<StudentMaster, String> {
    boolean existsByRegNo(String regNo);
}
