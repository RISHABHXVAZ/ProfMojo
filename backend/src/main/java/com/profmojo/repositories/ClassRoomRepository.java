package com.profmojo.repositories;

import com.profmojo.models.ClassRoom;
import com.profmojo.models.Professor;
import com.profmojo.models.dto.StudentClassDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {

    List<ClassRoom> findByProfessor(Professor professor);

    boolean existsByClassCode(String classCode);

    Optional<ClassRoom> findByClassCode(String classCode);




}
