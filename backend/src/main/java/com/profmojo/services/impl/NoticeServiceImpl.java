package com.profmojo.services.impl;

import com.profmojo.models.*;
import com.profmojo.models.dto.*;
import com.profmojo.repositories.*;
import com.profmojo.services.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepo;
    private final NoticeClassMappingRepository mappingRepo;
    private final ClassRoomRepository classRoomRepo;

    @Transactional
    @Override
    public void createNotice(CreateNoticeRequest req, Professor professor) {
        if (professor == null) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthenticated professor");
        }

        if (req.getClassCodes() == null || req.getClassCodes().isEmpty()) {
            throw new IllegalArgumentException("At least one class code is required to create a notice");
        }

        // Transactionally verify that all class codes belong to the authenticated professor
        for (String classCode : req.getClassCodes()) {
            ClassRoom classRoom = classRoomRepo.findByClassCode(classCode)
                    .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classCode));

            if (classRoom.getProfessor() == null || !classRoom.getProfessor().getProfId().equals(professor.getProfId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Unauthorized: You do not own class " + classCode
                );
            }
        }

        Notice notice = new Notice();
        notice.setTitle(req.getTitle());
        notice.setMessage(req.getMessage());
        notice.setCreatedBy(professor);
        notice.setCreatedAt(LocalDateTime.now());

        noticeRepo.save(notice);

        for (String classCode : req.getClassCodes()) {
            NoticeClassMapping map = new NoticeClassMapping();
            map.setNotice(notice);
            map.setClassCode(classCode);
            mappingRepo.save(map);
        }
    }

    public List<NoticeResponseDTO> getStudentNotices(List<String> classCodes) {
        return mappingRepo.findByClassCodeIn(classCodes)
                .stream()
                .map(m -> new NoticeResponseDTO(
                        m.getNotice().getTitle(),
                        m.getNotice().getMessage(),
                        m.getClassCode(),
                        m.getNotice().getCreatedAt(),
                        m.getNotice().getCreatedBy().getName()
                ))
                .distinct()
                .toList();
    }

    public List<NoticeResponseDTO> getProfessorNotices(Professor professor) {
        List<Notice> notices = noticeRepo.findByCreatedByOrderByCreatedAtDesc(professor);
        if (notices.isEmpty()) {
            return Collections.emptyList();
        }

        List<NoticeClassMapping> allMappings = mappingRepo.findByNoticeIn(notices);
        Map<Long, List<NoticeClassMapping>> mappingsByNoticeId = allMappings.stream()
                .collect(Collectors.groupingBy(m -> m.getNotice().getId()));

        String profName = professor != null ? professor.getName() : null;

        return notices.stream()
                .flatMap(n -> {
                    List<NoticeClassMapping> mappings = mappingsByNoticeId.getOrDefault(n.getId(), Collections.emptyList());
                    return mappings.stream()
                            .map(m -> new NoticeResponseDTO(
                                    n.getTitle(),
                                    n.getMessage(),
                                    m.getClassCode(),
                                    n.getCreatedAt(),
                                    profName
                            ));
                })
                .toList();
    }
}
