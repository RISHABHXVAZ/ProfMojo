package com.profmojo.services;

import com.profmojo.models.Attendance;
import com.profmojo.models.Professor;
import com.profmojo.models.dto.AttendanceStudentSummaryDTO;
import com.profmojo.models.dto.AttendanceSummaryDTO;
import com.profmojo.models.dto.StudentAttendanceSummaryDTO;
import com.profmojo.models.dto.StudentClassDTO;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    void markAttendance(
            Professor professor,
            String classCode,
            String studentRegNo,
            boolean present,
            LocalDate localDate
    );

    List<Attendance> getAttendanceForClassToday(String classCode, Professor professor);

    List<Attendance> getAttendanceForClassByDate(String classCode, LocalDate date, Professor professor);

    List<Attendance> getStudentAttendanceHistory(
            String classCode,
            String studentRegNo,
            Professor professor
    );

    AttendanceSummaryDTO getAttendanceSummary(String classCode, Professor professor);

    List<AttendanceStudentSummaryDTO> getStudentAttendanceSummary(String classCode, Professor professor);


    List<StudentAttendanceSummaryDTO> getStudentAttendance(String regNo);

    List<StudentClassDTO> getStudentClasses(String regNo);

    List<StudentAttendanceSummaryDTO> getStudentDashboard(String regNo);

    List<StudentAttendanceSummaryDTO> getMyAttendance(String regNo);
}
