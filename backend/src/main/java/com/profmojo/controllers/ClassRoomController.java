package com.profmojo.controllers;

import com.profmojo.models.ClassEnrollment;
import com.profmojo.models.ClassRoom;
import com.profmojo.models.Professor;
import com.profmojo.services.ClassRoomService;
import lombok.RequiredArgsConstructor;
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
    public ClassRoom createClass(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal Professor professor
    ){
        return classRoomService.createClass(request.get("className"), professor);
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
