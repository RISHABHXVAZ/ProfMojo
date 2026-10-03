package com.profmojo.services.impl;

import com.profmojo.models.*;
import com.profmojo.models.dto.AttendanceStudentSummaryDTO;
import com.profmojo.models.dto.AttendanceSummaryDTO;
import com.profmojo.models.dto.StudentAttendanceSummaryDTO;
import com.profmojo.models.dto.StudentClassDTO;
import com.profmojo.repositories.*;
import com.profmojo.services.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepo;
    private final ClassRoomRepository classRoomRepo;
    private final StudentClassRepository studentClassRepo;

    @Override
    public void markAttendance(String classCode, String studentRegNo, boolean present, LocalDate date) {

        Attendance attendance = attendanceRepo
                .findByClassCodeAndStudentRegNoAndAttendanceDate(
                        classCode, studentRegNo, date
                )
                .orElse(
                        Attendance.builder()
                                .classCode(classCode)
                                .studentRegNo(studentRegNo)
                                .attendanceDate(date)
                                .build()
                );

        attendance.setPresent(present);
        attendanceRepo.save(attendance);
    }





    @Override
    public List<Attendance> getAttendanceForClassToday(String classCode) {

        // optional safety check (recommended)
        classRoomRepo.findByClassCode(classCode)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        LocalDate today = LocalDate.now();

        return attendanceRepo.findByClassCodeAndAttendanceDate(classCode, today);
    }

    @Override
    public List<Attendance> getAttendanceForClassByDate(String classCode, LocalDate date) {

        classRoomRepo.findByClassCode(classCode)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        return attendanceRepo.findByClassCodeAndAttendanceDate(classCode, date);
    }

    @Override
    public List<Attendance> getStudentAttendanceHistory(
            String classCode,
            String studentRegNo
    ) {
        return attendanceRepo
                .findByClassCodeAndStudentRegNoOrderByAttendanceDateAsc(
                        classCode,
                        studentRegNo
                );
    }

    @Override
    public AttendanceSummaryDTO getAttendanceSummary(String classCode) {

        long totalLectures = attendanceRepo.countTotalLectures(classCode);
        Double avg = attendanceRepo.getAverageAttendance(classCode);
        long lowCount = attendanceRepo
                .findLowAttendanceStudents(classCode)
                .size();


        return new AttendanceSummaryDTO(
                totalLectures,
                avg != null ? Math.round(avg * 10.0) / 10.0 : 0,
                lowCount != 0 ? lowCount : 0
        );

    }

    @Override
    public List<AttendanceStudentSummaryDTO> getStudentAttendanceSummary(String classCode) {
        return attendanceRepo.getStudentAttendanceSummary(classCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentAttendanceSummaryDTO> getStudentAttendance(String regNo) {
        return attendanceRepo.getStudentAttendance(regNo);
    }

    @Override
    public List<StudentClassDTO> getStudentClasses(String regNo) {
        return studentClassRepo.findStudentClasses(regNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentAttendanceSummaryDTO> getStudentDashboard(String regNo) {
        return attendanceRepo.getStudentAttendance(regNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentAttendanceSummaryDTO> getMyAttendance(String regNo) {
        return attendanceRepo.getStudentAttendance(regNo);
    }
}
