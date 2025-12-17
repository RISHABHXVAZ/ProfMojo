package com.profmojo.controllers;

import com.profmojo.models.ClassEnrollment;
import com.profmojo.models.ClassRoom;
import com.profmojo.models.Professor;
import com.profmojo.models.dto.CreateClassResponseDTO;
import com.profmojo.services.ClassRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/professor/classes")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class ClassRoomController {

    private final ClassRoomService classRoomService;

    // Create class
    @PostMapping("/create")
    public ResponseEntity<CreateClassResponseDTO> createClass(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal Professor professor
    ){
        ClassRoom classRoom = classRoomService.createClass(
                request.get("className"),
                professor
        );

        return ResponseEntity.ok(
                new CreateClassResponseDTO(
                        classRoom.getClassCode(),
                        classRoom.getClassName()
                )
        );
    }

    // Get my classes
    @GetMapping("/my")
    public List<ClassRoom> getMyClasses(
            @AuthenticationPrincipal Professor professor
    ) {
        return classRoomService.getMyClasses(professor);
    }

    @GetMapping("/{classCode}/students")
    public List<ClassEnrollment> getStudentsOfClass(@PathVariable String classCode) {
        return classRoomService.getStudentsOfClass(classCode);
    }
}
