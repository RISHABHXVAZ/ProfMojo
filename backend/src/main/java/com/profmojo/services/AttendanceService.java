package com.profmojo.services;

import com.profmojo.models.Attendance;
import com.profmojo.models.dto.AttendanceStudentSummaryDTO;
import com.profmojo.models.dto.AttendanceSummaryDTO;
import com.profmojo.models.dto.StudentAttendanceSummaryDTO;
import com.profmojo.models.dto.StudentClassDTO;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    void markAttendance(
            String classCode,
            String studentRegNo,
            boolean present,
            LocalDate localDate
    );

    List<Attendance> getAttendanceForClassToday(String classCode);

    List<Attendance> getAttendanceForClassByDate(String classCode, LocalDate date);

    List<Attendance> getStudentAttendanceHistory(
            String classCode,
            String studentRegNo
    );
    AttendanceSummaryDTO getAttendanceSummary(String classCode);

    List<AttendanceStudentSummaryDTO> getStudentAttendanceSummary(String classCode);

    List<StudentAttendanceSummaryDTO> getStudentAttendance(String regNo);

    List<StudentClassDTO> getStudentClasses(String regNo);


}
