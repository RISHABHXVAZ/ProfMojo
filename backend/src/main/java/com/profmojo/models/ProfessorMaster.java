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
    private String profId; // professor enters this ID when registering

    @Column(name = "name")
    private String name;

    @Column(name = "department")
    private String department;
}
