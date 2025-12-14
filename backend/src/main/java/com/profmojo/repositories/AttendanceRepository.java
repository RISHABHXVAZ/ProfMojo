package com.profmojo.repositories;

import com.profmojo.models.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
