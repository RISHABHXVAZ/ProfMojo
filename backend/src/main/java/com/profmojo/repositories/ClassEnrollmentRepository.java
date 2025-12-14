package com.profmojo.repositories;

import com.profmojo.models.ClassEnrollment;
import com.profmojo.models.ClassRoom;
import com.profmojo.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassEnrollmentRepository
        extends JpaRepository<ClassEnrollment, Long> {

    List<ClassEnrollment> findByClassRoom_ClassCodeOrderByStudent_NameAsc(String classCode);

    List<ClassEnrollment> findByClassRoom_IdOrderByStudent_NameAsc(Long classId);

    boolean existsByClassRoomAndStudent(ClassRoom classRoom, Student student);

}
