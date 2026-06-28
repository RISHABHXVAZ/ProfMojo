package com.profmojo.config;

import com.profmojo.models.DepartmentSecret;
import com.profmojo.repositories.DepartmentSecretRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
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
            System.out.println(">>> [Seeder] No admin seeds found. Skipping.");
            return;
        }

        // 2. Split multiple departments if they are separated by commas
        String[] departments = seedData.split(",");

        for (String deptData : departments) {
            // 3. Split by colon to extract details -> DEPARTMENT:EMAIL:SECRET_KEY
            String[] details = deptData.split(":");
            if (details.length != 3) {
                System.err.println(">>> [Seeder] Invalid format for entry: " + deptData);
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
                System.out.println(">>> [Seeder] Successfully saved secret for department: " + departmentName);
            } else {
                System.out.println(">>> [Seeder] Department " + departmentName + " already exists. Skipping.");
            }
        }
    }
}