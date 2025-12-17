package com.profmojo.services.impl;

import com.profmojo.models.Student;
import com.profmojo.repositories.StudentMasterRepository;
import com.profmojo.repositories.StudentRepository;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.services.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepo;
    private final StudentMasterRepository masterRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    @Override
    public boolean canRegister(String regNo) {
        // regNo must exist in master AND not yet registered
        return masterRepo.existsByRegNo(regNo) && !studentRepo.existsByRegNo(regNo);
    }

    @Override
    public Student findByRegNo(String regNo) {   // ⭐ NEW METHOD
        return studentRepo.findByRegNo(regNo);
    }

    @Override
    public Student register(Student student) {
        if (!masterRepo.existsByRegNo(student.getRegNo())) {
            throw new RuntimeException("Registration number not found in master records!");
        }

        if (studentRepo.existsByRegNo(student.getRegNo())) {
            throw new RuntimeException("Student already registered!");
        }

        student.setPassword(encoder.encode(student.getPassword()));
        return studentRepo.save(student);
    }

    @Override
    public String login(String regNo, String password) {
        Student student = studentRepo.findById(regNo)
                .orElseThrow(() -> new RuntimeException("Invalid registration number!"));

        if (!encoder.matches(password, student.getPassword())) {
            throw new RuntimeException("Invalid password!");
        }

        // ADD THIS TEMP LOG
        System.out.println("GENERATING JWT FOR STUDENT: " + regNo);

        return jwtUtil.generateToken(student.getRegNo(), "STUDENT");
    }

}
