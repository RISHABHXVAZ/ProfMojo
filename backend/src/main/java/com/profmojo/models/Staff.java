package com.profmojo.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "staff")
public class Staff {

    @Id
    private String staffId;  // STF-CSE-12

    private String name;
    private String password;
    private String department;

    private boolean available = true;

    private String role = "STAFF";
}

