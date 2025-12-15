package com.profmojo.repositories;

import com.profmojo.models.Attendance;
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
        SELECT COUNT(DISTINCT a.studentRegNo)
        FROM Attendance a
        WHERE a.classCode = :classCode
        GROUP BY a.studentRegNo
        HAVING (SUM(CASE WHEN a.present = true THEN 1 ELSE 0 END) * 1.0 / COUNT(*)) < 0.75
    """)
    Long countLowAttendanceStudents(@Param("classCode") String classCode);
}
