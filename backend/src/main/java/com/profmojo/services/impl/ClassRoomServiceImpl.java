package com.profmojo.services.impl;

import com.profmojo.models.*;
import com.profmojo.models.dto.StudentClassDTO;
import com.profmojo.repositories.*;
import com.profmojo.services.ClassRoomService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassRoomServiceImpl implements ClassRoomService {

    private final ClassRoomRepository classRoomRepo;
    private final ClassEnrollmentRepository classEnrollmentRepo;
    private final StudentClassRepository studentClassRepo;
    private final StudentRepository studentRepo;
    private final AttendanceRepository attendanceRepo;
    @Override
    public ClassRoom createClass(String className, Professor professor) {

        ClassRoom room = new ClassRoom();
        room.setClassName(className);
        room.setProfessor(professor);
        room.setClassCode(generateClassCode(className));

        return classRoomRepo.save(room);
    }

    @Override
    public List<ClassRoom> getMyClasses(Professor professor) {
        return classRoomRepo.findByProfessor(professor);
    }

    private String generateClassCode(String className) {
        return className.replaceAll("\\s+", "").toUpperCase()
                + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    @Override
    public List<ClassEnrollment> getStudentsOfClass(String classCode, Professor professor) {
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

        // ✅ Fetch purely from enrollment table
        return classEnrollmentRepo.findByClassCode(classCode);
    }

    @Transactional
    public void joinClass(String classCode, Student student) {

        ClassRoom classRoom = classRoomRepo
                .findByClassCode(classCode)
                .orElseThrow(() -> new RuntimeException("Invalid class code"));

        boolean exists = classEnrollmentRepo
                .existsByClassCodeAndStudent_RegNo(classCode, student.getRegNo());

        if (exists) {
            throw new RuntimeException("Already joined");
        }

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setClassRoom(classRoom);
        enrollment.setStudent(student);
        enrollment.setClassCode(classCode);

        classEnrollmentRepo.save(enrollment);
    }

    @Transactional
    @Override
    public void deleteClass(String classCode, Professor professor) {
        if (professor == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unauthenticated: Professor identity is required"
            );
        }

        ClassRoom classRoom = classRoomRepo
                .findByClassCode(classCode)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classCode));

        // 🔐 SECURITY CHECK
        if (classRoom.getProfessor() == null || !classRoom.getProfessor().getProfId().equals(professor.getProfId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unauthorized: You do not own class " + classCode
            );
        }

        // 1️⃣ Delete attendance
        attendanceRepo.deleteByClassCode(classCode);

        // 2️⃣ Delete enrollments
        classEnrollmentRepo.deleteByClassCode(classCode);

        // 3️⃣ Delete class itself
        classRoomRepo.delete(classRoom);
    }



}
