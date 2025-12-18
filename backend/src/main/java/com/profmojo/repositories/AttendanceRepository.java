package com.profmojo.repositories;

import com.profmojo.models.Attendance;
import com.profmojo.models.dto.AttendanceStudentSummaryDTO;
import com.profmojo.models.dto.StudentAttendanceSummaryDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByClassCodeAndStudentRegNoAndAttendanceDate(
            String classCode,
            String studentRegNo,
            LocalDate attendanceDate
    );

    List<Attendance> findByClassCodeAndAttendanceDate(
            String classCode,
            LocalDate attendanceDate
    );

    List<Attendance> findByClassCodeAndStudentRegNoOrderByAttendanceDateAsc(
            String classCode,
            String studentRegNo
    );

    // 1️⃣ Total lectures = distinct dates
    @Query("""
        SELECT COUNT(DISTINCT a.attendanceDate)
        FROM Attendance a
        WHERE a.classCode = :classCode
    """)
    long countTotalLectures(@Param("classCode") String classCode);

    // 2️⃣ Average attendance %
    @Query("""
        SELECT AVG(
            CASE WHEN a.present = true THEN 1.0 ELSE 0.0 END
        ) * 100
        FROM Attendance a
        WHERE a.classCode = :classCode
    """)
    Double getAverageAttendance(@Param("classCode") String classCode);

    // 3️⃣ Low attendance students (<75%)
    @Query("""
    SELECT a.studentRegNo
    FROM Attendance a
    WHERE a.classCode = :classCode
    GROUP BY a.studentRegNo
    HAVING (SUM(CASE WHEN a.present = true THEN 1 ELSE 0 END) * 1.0 / COUNT(*)) < 0.75
""")
    List<String> findLowAttendanceStudents(@Param("classCode") String classCode);


    @Query("""
        SELECT new com.profmojo.models.dto.AttendanceStudentSummaryDTO(
            a.studentRegNo,
            COUNT(a),
            SUM(CASE WHEN a.present = true THEN 1 ELSE 0 END),
            (SUM(CASE WHEN a.present = true THEN 1 ELSE 0 END) * 100.0 / COUNT(a)),
            (SUM(CASE WHEN a.present = true THEN 1 ELSE 0 END) * 1.0 / COUNT(a)) < 0.75
        )
        FROM Attendance a
        WHERE a.classCode = :classCode
        GROUP BY a.studentRegNo
    """)
    List<AttendanceStudentSummaryDTO> getStudentAttendanceSummary(String classCode);

    @Query("""
SELECT COUNT(a.id),
       SUM(CASE WHEN a.present = true THEN 1 ELSE 0 END)
FROM Attendance a
WHERE a.classCode = :classCode
  AND a.studentRegNo = :regNo
""")
    List<Object[]> getStudentAttendanceStats(
            @Param("classCode") String classCode,
            @Param("regNo") String regNo
    );



    @Query("""
SELECT DISTINCT a.classCode
FROM Attendance a
WHERE a.studentRegNo = :regNo
""")
    List<String> findDistinctClassCodesByStudent(@Param("regNo") String regNo);

    void deleteByClassCode(String classCode);

    long countDistinctByClassCode(String classCode);

    long countByClassCodeAndStudentRegNoAndPresentTrue(
            String classCode,
            String studentRegNo
    );

    @Query("""
SELECT new com.profmojo.models.dto.StudentAttendanceSummaryDTO(
    cr.classCode,
    cr.className,
    p.name,
    COUNT(DISTINCT a.attendanceDate),
    COALESCE(SUM(CASE WHEN a.present = true THEN 1 ELSE 0 END), 0)
)
FROM ClassEnrollment ce
JOIN ce.classRoom cr
JOIN cr.professor p
LEFT JOIN Attendance a
    ON a.classCode = cr.classCode
    AND a.studentRegNo = ce.student.regNo
WHERE ce.student.regNo = :regNo
GROUP BY cr.classCode, cr.className, p.name
""")
    List<StudentAttendanceSummaryDTO> getStudentAttendance(@Param("regNo") String regNo);

}
