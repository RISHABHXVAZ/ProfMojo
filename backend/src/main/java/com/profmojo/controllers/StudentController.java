package com.profmojo.controllers;

import com.profmojo.models.Student;
import com.profmojo.repositories.StudentRepository;
import com.profmojo.services.ClassRoomService;
import com.profmojo.services.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class StudentController {

    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final ClassRoomService classRoomService;

    @GetMapping("/check-id/{regNo}")
    public ResponseEntity<?> checkRegistrationNumber(@PathVariable String regNo) {
        boolean available = studentService.canRegister(regNo);
        return ResponseEntity.ok(Map.of("canRegister", available));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Student student) {
        Student saved = studentService.register(student);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String token = studentService.login(
                request.get("regNo"),
                request.get("password")
        );
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(HttpServletRequest request) {
        String regNo = (String) request.getAttribute("username");

        Student student = studentRepository.findById(regNo).orElse(null);

        if (student == null) {
            return ResponseEntity.status(404).body("Student not found");
        }

        return ResponseEntity.ok(student);
    }

    @PostMapping("/join/{classCode}")
    public ResponseEntity<?> joinClass(
            @PathVariable String classCode,
            HttpServletRequest request
    ) {
        String regNo = (String) request.getAttribute("username");

        Student student = studentRepository.findById(regNo)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        classRoomService.joinClass(classCode, student);
        return ResponseEntity.ok().build();
    }



}
