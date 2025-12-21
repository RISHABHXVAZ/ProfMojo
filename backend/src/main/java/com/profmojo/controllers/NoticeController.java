package com.profmojo.controllers;

import com.profmojo.models.*;
import com.profmojo.models.dto.*;
import com.profmojo.repositories.ClassEnrollmentRepository;
import com.profmojo.services.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final ClassEnrollmentRepository enrollmentRepo;

    /* PROFESSOR */
    @PostMapping("/create")
    public void createNotice(
            @RequestBody CreateNoticeRequest req,
            @AuthenticationPrincipal Professor professor
    ) {
        noticeService.createNotice(req, professor);
    }

    /* PROFESSOR – MY NOTICES */
    @GetMapping("/professor/my")
    public List<NoticeResponseDTO> getProfessorNotices(
            @AuthenticationPrincipal Professor professor
    ) {
        return noticeService.getProfessorNotices(professor);
    }

    /* STUDENT */
    @GetMapping("/student/my")
    public List<NoticeResponseDTO> getStudentNotices(
            @AuthenticationPrincipal Student student
    ) {
        List<String> classCodes = enrollmentRepo
                .findClassCodesByStudent(student.getRegNo());

        return noticeService.getStudentNotices(classCodes);
    }
}
