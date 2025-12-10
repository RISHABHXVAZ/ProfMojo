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
    private String profId; // professor enters this ID when registering

    private String name;
    private String department;
}
