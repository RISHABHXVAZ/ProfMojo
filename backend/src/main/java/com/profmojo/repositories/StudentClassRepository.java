package com.profmojo.repositories;

import com.profmojo.models.StudentClass;
import com.profmojo.models.dto.StudentClassDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentClassRepository
        extends JpaRepository<StudentClass, Long> {

    List<StudentClass> findByStudentRegNo(String studentRegNo);

    List<StudentClass> findByClassCode(String classCode);

    boolean existsByStudentRegNoAndClassCode(
            String studentRegNo,
            String classCode
    );

    @Query("""
SELECT new com.profmojo.models.dto.StudentClassDTO(
    cr.classCode,
    cr.className,
    p.name,
    COUNT(a.id),
    COALESCE(SUM(CASE WHEN a.present = true THEN 1 ELSE 0 END), 0)
)
FROM StudentClass sc
JOIN ClassRoom cr ON cr.classCode = sc.classCode
JOIN cr.professor p
LEFT JOIN Attendance a
    ON a.classCode = sc.classCode
   AND a.studentRegNo = sc.studentRegNo
WHERE sc.studentRegNo = :regNo
GROUP BY cr.classCode, cr.className, p.name
""")
    List<StudentClassDTO> findStudentClasses(@Param("regNo") String regNo);

}
