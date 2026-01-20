package com.profmojo.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "staff")
@JsonIgnoreProperties({"password", "hibernateLazyInitializer", "handler"})
public class Staff {

    @Id
    private String staffId;  // STF-CSE-12

    private String name;
    private String password;
    private String department;
    private String contactNo;


    private boolean available = true;
    private boolean passwordSet = false;
    private boolean online = true;

    @Column(nullable = true)
    private Integer stars = 0;

    @Column(nullable = true)
    private Integer totalDeliveries = 0;


    private String role = "STAFF";
}

