package com.profmojo.repositories;

import com.profmojo.models.ClassEnrollment;
import com.profmojo.models.ClassRoom;
import com.profmojo.models.Student;
import com.profmojo.models.dto.StudentAttendanceSummaryDTO;
import com.profmojo.models.dto.StudentClassDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassEnrollmentRepository
        extends JpaRepository<ClassEnrollment, Long> {

    List<ClassEnrollment> findByClassRoom_ClassCodeOrderByStudent_NameAsc(String classCode);

    List<ClassEnrollment> findByClassRoom_IdOrderByStudent_NameAsc(Long classId);

    boolean existsByClassRoomAndStudent(ClassRoom classRoom, Student student);

    @Query("""
    SELECT ce
    FROM ClassEnrollment ce
    JOIN FETCH ce.student
    WHERE ce.classCode = :classCode
""")
    List<ClassEnrollment> findByClassCode(@Param("classCode") String classCode);

    boolean existsByClassCodeAndStudent_RegNo(
            String classCode,
            String studentRegNo
    );

    void deleteByClassCode(String classCode);

}

