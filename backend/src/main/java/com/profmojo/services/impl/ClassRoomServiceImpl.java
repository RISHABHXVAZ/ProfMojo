package com.profmojo.services.impl;

import com.profmojo.models.ClassEnrollment;
import com.profmojo.models.ClassRoom;
import com.profmojo.models.Professor;
import com.profmojo.models.Student;
import com.profmojo.repositories.ClassEnrollmentRepository;
import com.profmojo.repositories.ClassRoomRepository;
import com.profmojo.services.ClassRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassRoomServiceImpl implements ClassRoomService {

    private final ClassRoomRepository classRoomRepo;
    private final ClassEnrollmentRepository enrollmentRepo;

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

        ClassRoom room = classRoomRepo
                .findByClassCode(classCode)
                .orElseThrow(() -> new RuntimeException("Invalid class code"));

        boolean alreadyJoined = enrollmentRepo
                .existsByClassRoomAndStudent(room, student);

        if (alreadyJoined) {
            throw new RuntimeException("Already joined this class");
        }

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setClassRoom(room);
        enrollment.setStudent(student);
        enrollment.setClassCode(room.getClassCode());

        enrollmentRepo.save(enrollment);
    }

}
