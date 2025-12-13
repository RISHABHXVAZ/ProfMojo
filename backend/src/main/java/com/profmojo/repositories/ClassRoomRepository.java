package com.profmojo.repositories;

import com.profmojo.models.ClassRoom;
import com.profmojo.models.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {

    List<ClassRoom> findByProfessor(Professor professor);

    boolean existsByClassCode(String classCode);
}
