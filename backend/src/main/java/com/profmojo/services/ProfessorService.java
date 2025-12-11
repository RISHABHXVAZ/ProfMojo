package com.profmojo.services;

import com.profmojo.models.Professor;
import com.profmojo.models.dto.LoginRequest;

public interface ProfessorService {

    Professor registerProfessor(Professor professor);

    String login(LoginRequest loginRequest);

    boolean existsByProfId(String profId);

}
