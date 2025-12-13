package com.profmojo.repositories;

import com.profmojo.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, String> {
    boolean existsByRegNo(String regNo);
    Student findByRegNo(String regNo);
}
