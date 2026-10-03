package com.profmojo.config;

import com.profmojo.models.DepartmentSecret;
import com.profmojo.repositories.DepartmentSecretRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@Slf4j
public class DepartmentSecretSeeder implements CommandLineRunner {

    private final DepartmentSecretRepository repository;

    @Value("${app.admin.department.seeds:}")
    private String seedData;

    // Spring automatically injects your DepartmentSecretRepository here
    public DepartmentSecretSeeder(DepartmentSecretRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. If no seed data environment variable is provided, stop here
        if (seedData == null || seedData.isBlank()) {
            log.info("No admin department seeds configured. Skipping seeder.");
            return;
        }

        // 2. Split multiple departments if they are separated by commas
        String[] departments = seedData.split(",");

        for (String deptData : departments) {
            // 3. Split by colon to extract details -> DEPARTMENT:EMAIL:SECRET_KEY
            String[] details = deptData.split(":");
            if (details.length != 3) {
                log.error("Invalid format for admin department seed entry; expected DEPARTMENT:EMAIL:SECRET");
                continue;
            }

            String departmentName = details[0].trim();
            String adminEmail = details[1].trim();
            String secretKey = details[2].trim();

            // 4. Check if this department already exists in the database table
            Optional<DepartmentSecret> existing = repository.findByDepartment(departmentName);

            // 5. If it doesn't exist, insert it securely
            if (existing.isEmpty()) {
                DepartmentSecret newSecret = new DepartmentSecret();
                newSecret.setDepartment(departmentName);
                newSecret.setAdminEmail(adminEmail);
                newSecret.setSecretKey(secretKey);
                newSecret.setActive(true);

                repository.save(newSecret);
                log.info("Successfully seeded secret for department: {}", departmentName);
            } else {
                DepartmentSecret current = existing.get();
                if (!current.getSecretKey().equals(secretKey) || !current.getAdminEmail().equals(adminEmail) || !current.isActive()) {
                    repository.delete(current);
                    DepartmentSecret updated = new DepartmentSecret();
                    updated.setDepartment(departmentName);
                    updated.setAdminEmail(adminEmail);
                    updated.setSecretKey(secretKey);
                    updated.setActive(true);
                    repository.save(updated);
                    log.info("Successfully updated secret for department: {}", departmentName);
                } else {
                    log.info("Department {} already up to date.", departmentName);
                }
            }
        }
    }
}