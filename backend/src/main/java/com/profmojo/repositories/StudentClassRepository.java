package com.profmojo.repositories;

import com.profmojo.models.StudentClass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentClassRepository
        extends JpaRepository<StudentClass, Long> {

    boolean existsByStudentRegNoAndClassCode(
            String studentRegNo,
            String classCode
    );
}
