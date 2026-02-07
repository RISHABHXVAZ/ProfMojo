package com.profmojo.controllers;

import com.profmojo.models.Professor;
import com.profmojo.models.dto.LoginRequest;
import com.profmojo.repositories.ProfessorMasterRepository;
import com.profmojo.repositories.ProfessorRepository;
import com.profmojo.services.ClassRoomService;
import com.profmojo.services.ProfessorService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/professors")
@RequiredArgsConstructor
@CrossOrigin(
        origins = "http://localhost:5173",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST}
)
public class ProfessorController {

    private final ProfessorService professorService;
    private final ProfessorMasterRepository professorMasterRepository;
    private final ProfessorRepository professorRepository;
    private final ClassRoomService classRoomService;


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String token = professorService.login(request);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(HttpServletRequest request) {

        String professorId = (String) request.getAttribute("username");

        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new RuntimeException("Professor not found"));

        return ResponseEntity.ok(
                Map.of(
                        "profId", professor.getProfId(),
                        "name", professor.getName(),
                        "contactNo", professor.getContactNo(),
                        "email", professor.getEmail()
                )
        );
    }


    @DeleteMapping("/{classCode}")
    public ResponseEntity<?> deleteClass(
            @PathVariable String classCode,
            @AuthenticationPrincipal Professor professor
    ) {
        classRoomService.deleteClass(classCode, professor);
        return ResponseEntity.ok().build();
    }

}
