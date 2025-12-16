package com.profmojo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
