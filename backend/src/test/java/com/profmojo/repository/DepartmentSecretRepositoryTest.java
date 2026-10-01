package com.profmojo.repository;

import com.profmojo.integration.BasePostgresContainerTest;
import com.profmojo.models.DepartmentSecret;
import com.profmojo.repositories.DepartmentSecretRepository;
import com.profmojo.testutil.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("DepartmentSecretRepository PostgreSQL Integration Tests")
class DepartmentSecretRepositoryTest extends BasePostgresContainerTest {

    @Autowired
    private DepartmentSecretRepository repository;

    @Test
    @DisplayName("Save and findByDepartment against real PostgreSQL container")
    void saveAndFindByDepartment_Success() {
        DepartmentSecret secret = TestDataFactory.createDepartmentSecret(
                "SECRET_ECE_2026",
                "ECE",
                "ece.admin@test.edu",
                true
        );

        DepartmentSecret saved = repository.save(secret);
        assertNotNull(saved);

        Optional<DepartmentSecret> found = repository.findByDepartment("ECE");
        assertTrue(found.isPresent());
        assertEquals("SECRET_ECE_2026", found.get().getSecretKey());
        assertEquals("ece.admin@test.edu", found.get().getAdminEmail());
        assertTrue(found.get().isActive());
    }

    @Test
    @DisplayName("findBySecretKey queries by primary key successfully")
    void findBySecretKey_Success() {
        DepartmentSecret secret = TestDataFactory.createDepartmentSecret(
                "SECRET_MECH_2026",
                "MECH",
                "mech.admin@test.edu",
                true
        );
        repository.save(secret);

        Optional<DepartmentSecret> found = repository.findBySecretKey("SECRET_MECH_2026");
        assertTrue(found.isPresent());
        assertEquals("MECH", found.get().getDepartment());
    }
}
