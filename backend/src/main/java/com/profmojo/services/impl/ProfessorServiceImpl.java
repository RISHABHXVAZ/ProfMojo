package com.profmojo.services.impl;

import com.profmojo.models.Professor;
import com.profmojo.models.dto.LoginRequest;
import com.profmojo.repositories.ProfessorMasterRepository;
import com.profmojo.repositories.ProfessorRepository;
import com.profmojo.security.jwt.JwtUtil;
import com.profmojo.services.ProfessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfessorServiceImpl implements ProfessorService {

    private final ProfessorRepository professorRepository;
    private final ProfessorMasterRepository professorMasterRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public Professor registerProfessor(Professor professor) {

        // 1️⃣ Check if profId exists in master table
        if (!professorMasterRepository.existsById(professor.getProfId())) {
            throw new RuntimeException("Professor ID not found in master records.");
        }

        // 2️⃣ Check if email is already used
        if (professorRepository.existsByEmail(professor.getEmail())) {
            throw new RuntimeException("Email is already registered.");
        }

        // 3️⃣ Encrypt password
        professor.setPassword(passwordEncoder.encode(professor.getPassword()));

        // ️Set default role (best practice)
        professor.setRole("PROFESSOR");



        // 5️⃣ Save the professor
        return professorRepository.save(professor);
    }



    @Override
    public String login(LoginRequest loginRequest) {

        // 1️⃣ Fetch professor using profId
        Professor prof = professorRepository.findById(loginRequest.getProfId())
                .orElseThrow(() -> new RuntimeException("Invalid Professor ID."));

        // 2️⃣ Validate password
        if (!passwordEncoder.matches(loginRequest.getPassword(), prof.getPassword())) {
            throw new RuntimeException("Invalid password!");
        }

        // 3️⃣ Generate JWT token
        return jwtUtil.generateToken(
                prof.getProfId(),     // subject
                prof.getRole()        // role
        );
    }

    @Override
    public boolean existsByProfId(String profId) {
        return professorRepository.existsByProfId(profId);
    }
}