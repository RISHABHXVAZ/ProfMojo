package com.profmojo.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessorMaster {

    @Id
    @Column(name = "prof_id")
    private String profId;

    private String name;

    private String department;

    @Column(unique = false, nullable = false)
    private String email;
}

