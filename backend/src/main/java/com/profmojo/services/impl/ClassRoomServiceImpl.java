package com.profmojo.services.impl;

import com.profmojo.models.ClassRoom;
import com.profmojo.models.Professor;
import com.profmojo.repositories.ClassRoomRepository;
import com.profmojo.services.ClassRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassRoomServiceImpl implements ClassRoomService {

    private final ClassRoomRepository classRoomRepo;

    @Override
    public ClassRoom createClass(String className, Professor professor) {

        ClassRoom room = new ClassRoom();
        room.setClassName(className);
        room.setProfessor(professor);
        room.setClassCode(generateClassCode(className));

        return classRoomRepo.save(room);
    }

    @Override
    public List<ClassRoom> getMyClasses(Professor professor) {
        return classRoomRepo.findByProfessor(professor);
    }

    private String generateClassCode(String className) {
        return className.replaceAll("\\s+", "").toUpperCase()
                + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
