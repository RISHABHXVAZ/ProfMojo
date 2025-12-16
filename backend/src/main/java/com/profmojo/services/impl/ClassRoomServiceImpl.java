package com.profmojo.services.impl;

import com.profmojo.models.*;
import com.profmojo.repositories.ClassEnrollmentRepository;
import com.profmojo.repositories.ClassRoomRepository;
import com.profmojo.repositories.StudentClassRepository;
import com.profmojo.repositories.StudentRepository;
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
    private final ClassEnrollmentRepository enrollmentRepo;
    private final StudentClassRepository studentClassRepo;

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

    public List<ClassEnrollment> getStudentsOfClass(String classCode) {
        classRoomRepo.findByClassCode(classCode)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        return enrollmentRepo
                .findByClassRoom_ClassCodeOrderByStudent_NameAsc(classCode);
    }

    @Override
    public void joinClass(String classCode, Student student) {

        // 1️⃣ Validate class exists
        ClassRoom classroom = classRoomRepo.findByClassCode(classCode)
                .orElseThrow(() ->
                        new RuntimeException("Invalid class code")
                );

        // 2️⃣ Prevent duplicate join
        boolean alreadyJoined =
                studentClassRepo.existsByStudentRegNoAndClassCode(
                        student.getRegNo(),
                        classCode
                );

        if (alreadyJoined) {
            throw new RuntimeException("You have already joined this class");
        }

        // 3️⃣ Save mapping
        StudentClass join = StudentClass.builder()
                .studentRegNo(student.getRegNo())
                .classCode(classCode)
                .build();

        studentClassRepo.save(join);
    }
}
