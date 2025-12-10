package com.profmojo.controllers;

import com.profmojo.models.Professor;
import com.profmojo.models.dto.LoginRequest;
import com.profmojo.services.ProfessorService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/professors")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class ProfessorController {

    private final ProfessorService professorService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Professor professor) {
        Professor saved = professorService.registerProfessor(professor);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String token = professorService.login(request);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        return ResponseEntity.ok("You are authenticated!");
    }
}
