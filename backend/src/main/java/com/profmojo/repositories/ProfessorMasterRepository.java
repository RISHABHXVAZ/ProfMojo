package com.profmojo.repositories;

import com.profmojo.models.Professor;
import com.profmojo.models.ProfessorMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfessorMasterRepository extends JpaRepository<ProfessorMaster, String> {

}
