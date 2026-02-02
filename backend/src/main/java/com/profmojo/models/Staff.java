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
    private String staffId;

    private String name;
    private String password;
    private String department;
    private String contactNo;

    @Builder.Default
    private boolean available = true;

    @Column(unique = true, nullable = false)
    private String email;

    @Builder.Default
    private boolean online = false;

    @Builder.Default
    private Integer stars = 0;

    @Builder.Default
    private Integer totalDeliveries = 0;

    @Builder.Default
    private String role = "STAFF";
}

