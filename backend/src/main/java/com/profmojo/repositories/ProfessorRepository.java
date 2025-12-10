package com.profmojo.repositories;

import com.profmojo.models.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessorRepository extends JpaRepository<Professor, String> {

    boolean existsByEmail(String email);

    Professor findByProfId(String profId); // for login using profId
}
