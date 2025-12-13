package com.profmojo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "student_master")
@Data
public class StudentMaster {
    @Id
    @Column(name = "reg_no")
    private String regNo;

    private String name;
    private String branch;
    private String year;
}
