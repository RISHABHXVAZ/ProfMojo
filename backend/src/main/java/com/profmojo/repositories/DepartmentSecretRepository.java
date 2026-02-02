package com.profmojo.repositories;

import com.profmojo.models.DepartmentSecret;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DepartmentSecretRepository
        extends JpaRepository<DepartmentSecret, String> {

    Optional<DepartmentSecret> findBySecretKey(String secretKey);
}

