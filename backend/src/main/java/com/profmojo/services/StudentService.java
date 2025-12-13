package com.profmojo.services;

import com.profmojo.models.Student;

public interface StudentService {
    Student register(Student student);
    String login(String regNo, String password);
    boolean canRegister(String regNo);

    Student findByRegNo(String regNo);
}
