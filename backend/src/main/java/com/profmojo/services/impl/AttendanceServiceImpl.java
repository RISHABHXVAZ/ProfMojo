package com.profmojo.services.impl;

import com.profmojo.models.*;
import com.profmojo.repositories.*;
import com.profmojo.services.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepo;
    private final ClassRoomRepository classRoomRepo;
    private final StudentRepository studentRepo;

    @Override
    public void markAttendance(String classCode, String studentRegNo, boolean present) {

        ClassRoom room = classRoomRepo.findByClassCode(classCode)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        Student student = studentRepo.findById(studentRegNo)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepo
                .findByClassRoomAndStudentAndDate(room, student, today)
                .orElse(new Attendance());

        attendance.setClassRoom(room);
        attendance.setStudent(student);
        attendance.setDate(today);
        attendance.setPresent(present);

        attendanceRepo.save(attendance);
    }

    @Override
    public List<Attendance> getAttendanceForClassToday(String classCode) {

        ClassRoom room = classRoomRepo.findByClassCode(classCode)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        return attendanceRepo.findByClassRoomAndDate(room, LocalDate.now());
    }
}
