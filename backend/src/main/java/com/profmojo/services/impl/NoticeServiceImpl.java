package com.profmojo.services.impl;

import com.profmojo.models.*;
import com.profmojo.models.dto.*;
import com.profmojo.repositories.*;
import com.profmojo.services.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeRepository noticeRepo;
    private final NoticeClassMappingRepository mappingRepo;
    private final ClassRoomRepository classRoomRepo;

    public void createNotice(CreateNoticeRequest req, Professor professor) {
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

        return noticeRepo.findByCreatedByOrderByCreatedAtDesc(professor)
                .stream()
                .flatMap(n ->
                        mappingRepo.findByNotice(n).stream()
                                .map(m -> new NoticeResponseDTO(
                                        n.getTitle(),
                                        n.getMessage(),
                                        m.getClassCode(),
                                        n.getCreatedAt(),
                                        professor.getName()
                                ))
                )
                .toList();
    }
}
