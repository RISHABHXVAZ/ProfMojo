package com.profmojo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Student {
    @Id
    private String regNo;

    private String name;
    private String password;
    private String branch;
    private String year;
    private String contactNo;

    private String role = "STUDENT";
}
