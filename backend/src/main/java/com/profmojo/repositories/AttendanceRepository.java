package com.profmojo.repositories;

import com.profmojo.models.Attendance;
import com.profmojo.models.ClassRoom;
import com.profmojo.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByClassRoomAndStudentAndDate(
            ClassRoom classRoom,
            Student student,
            LocalDate date
    );

    List<Attendance> findByClassRoomAndDate(ClassRoom classRoom, LocalDate date);
}
