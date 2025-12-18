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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepo;
    private final ClassRoomRepository classRoomRepo;
    private final StudentRepository studentRepo;
    private final StudentClassRepository studentClassRepo;
    private final ClassEnrollmentRepository classEnrollmentRepo;

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
    public List<StudentAttendanceSummaryDTO> getStudentAttendance(String regNo) {

        Student student = studentRepo.findById(regNo)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<String> classCodes =
                attendanceRepo.findDistinctClassCodesByStudent(regNo);

        List<StudentAttendanceSummaryDTO> result = new ArrayList<>();

        for (String classCode : classCodes) {

            ClassRoom cls = classRoomRepo.findByClassCode(classCode)
                    .orElseThrow(() -> new RuntimeException("Class not found"));

            List<Object[]> rows =
                    attendanceRepo.getStudentAttendanceStats(classCode, regNo);

            Object[] stats = rows.get(0);   // ← THE ACTUAL ROW


            long total = stats[0] == null ? 0L : ((Number) stats[0]).longValue();
            long present = stats[1] == null ? 0L : ((Number) stats[1]).longValue();

            double percentage = total == 0 ? 0.0 : (present * 100.0) / total;


            result.add(new StudentAttendanceSummaryDTO(
                    cls.getClassCode(),
                    cls.getClassName(),
                    cls.getProfessor().getName(),
                    total,
                    present,
                    Math.round(percentage * 10.0) / 10.0
            ));
        }

        return result;
    }


    @Override
    public List<StudentClassDTO> getStudentClasses(String regNo) {
        return studentClassRepo.findStudentClasses(regNo);
    }

    public List<StudentAttendanceSummaryDTO> getStudentDashboard(String regNo) {

        // 1️⃣ Get all classes student joined
        List<ClassEnrollment> enrollments =
                classEnrollmentRepo.findByStudent_RegNo(regNo);

        // 2️⃣ For each class, compute attendance
        return enrollments.stream().map(enrollment -> {
            String classCode = enrollment.getClassCode();

            long totalLectures =
                    attendanceRepo.countDistinctByClassCode(classCode);

            long presentCount =
                    attendanceRepo.countByClassCodeAndStudentRegNoAndPresentTrue(
                            classCode, regNo
                    );

            double percentage =
                    totalLectures == 0 ? 0 :
                            (presentCount * 100.0) / totalLectures;

            return new StudentAttendanceSummaryDTO(
                    enrollment.getClassRoom().getClassCode(),
                    enrollment.getClassRoom().getClassName(),
                    enrollment.getClassRoom().getProfessor().getName(),
                    presentCount,
                    totalLectures,
                    (int) percentage
            );
        }).toList();
    }


    @Override
    public List<StudentAttendanceSummaryDTO> getMyAttendance(String regNo) {
        return attendanceRepo.getStudentAttendance(regNo);
    }


}
