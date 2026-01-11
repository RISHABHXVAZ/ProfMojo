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
@Table(name = "admin")
public class Admin {

    @Id
    private String adminId;   // e.g. "admin_cse"

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    private String department;   // CSE, ECE, ME, etc.

    private boolean active = true;
}








