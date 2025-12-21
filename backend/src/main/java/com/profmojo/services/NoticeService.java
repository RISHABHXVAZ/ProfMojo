package com.profmojo.services;

import com.profmojo.models.Professor;
import com.profmojo.models.dto.CreateNoticeRequest;
import com.profmojo.models.dto.NoticeResponseDTO;

import java.util.List;

public interface NoticeService {
    void createNotice(CreateNoticeRequest req, Professor professor);

    List<NoticeResponseDTO> getStudentNotices(List<String> classCodes);

    List<NoticeResponseDTO> getProfessorNotices(Professor professor);

}
