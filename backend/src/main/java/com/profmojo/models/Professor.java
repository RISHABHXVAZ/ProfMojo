package com.profmojo.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table
public class Professor {

    @Id
    private String profId; // professor logs in using this

    private String name;

    @Column(unique = true)
    private String email;

    private String password; // will hash later

    private String department;

    private String contactNo;

    private String role = "PROFESSOR";
}
