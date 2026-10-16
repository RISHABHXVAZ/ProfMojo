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
    private final ClassEnrollmentRepository classEnrollmentRepo;

    private ClassRoom validateProfessorOwnership(String classCode, Professor professor) {
        if (professor == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unauthenticated: Professor identity is required"
            );
        }

        ClassRoom classRoom = classRoomRepo.findByClassCode(classCode)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classCode));

        if (classRoom.getProfessor() == null || !classRoom.getProfessor().getProfId().equals(professor.getProfId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unauthorized: You do not own class " + classCode
            );
        }
        return classRoom;
    }

    private void validateStudentInClass(String classCode, String studentRegNo) {
        boolean enrolled = classEnrollmentRepo.existsByClassCodeAndStudent_RegNo(classCode, studentRegNo)
                || studentClassRepo.existsByStudentRegNoAndClassCode(studentRegNo, classCode);
        if (!enrolled) {
            throw new IllegalArgumentException(
                    "Student " + studentRegNo + " is not enrolled in class " + classCode
            );
        }
    }

    @Override
    public void markAttendance(Professor professor, String classCode, String studentRegNo, boolean present, LocalDate date) {
        validateProfessorOwnership(classCode, professor);
        validateStudentInClass(classCode, studentRegNo);

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
    public List<Attendance> getAttendanceForClassToday(String classCode, Professor professor) {
        validateProfessorOwnership(classCode, professor);

        LocalDate today = LocalDate.now();

        return attendanceRepo.findByClassCodeAndAttendanceDate(classCode, today);
    }

    @Override
    public List<Attendance> getAttendanceForClassByDate(String classCode, LocalDate date, Professor professor) {
        validateProfessorOwnership(classCode, professor);

        return attendanceRepo.findByClassCodeAndAttendanceDate(classCode, date);
    }

    @Override
    public List<Attendance> getStudentAttendanceHistory(
            String classCode,
            String studentRegNo,
            Professor professor
    ) {
        validateProfessorOwnership(classCode, professor);
        validateStudentInClass(classCode, studentRegNo);

        return attendanceRepo
                .findByClassCodeAndStudentRegNoOrderByAttendanceDateAsc(
                        classCode,
                        studentRegNo
                );
    }

    @Override
    public AttendanceSummaryDTO getAttendanceSummary(String classCode, Professor professor) {
        validateProfessorOwnership(classCode, professor);

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
    public List<AttendanceStudentSummaryDTO> getStudentAttendanceSummary(String classCode, Professor professor) {
        validateProfessorOwnership(classCode, professor);

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
