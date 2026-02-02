package com.profmojo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "department_secret")
public class DepartmentSecret {

    @Id
    private String secretKey;   // e.g. CSE-ADMIN-2026

    @Column(nullable = false)
    private String department;  // CSE, ECE, Arts, etc.

    @Column(nullable = false)
    private String adminEmail;

    private boolean active = true;
}
