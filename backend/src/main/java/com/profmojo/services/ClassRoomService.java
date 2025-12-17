package com.profmojo.services;

import com.profmojo.models.ClassEnrollment;
import com.profmojo.models.ClassRoom;
import com.profmojo.models.Professor;
import com.profmojo.models.Student;
import com.profmojo.models.dto.StudentClassDTO;

import java.util.List;

public interface ClassRoomService {

    ClassRoom createClass(String className, Professor professor);

    List<ClassRoom> getMyClasses(Professor professor);

    List<ClassEnrollment> getStudentsOfClass(String classCode);

    void joinClass(String classCode, Student student);

    void deleteClass(String classCode, Professor professor);


}
