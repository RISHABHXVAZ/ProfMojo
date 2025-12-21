package com.profmojo.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NoticeResponseDTO {
    private String title;
    private String message;
    private String classCode;
    private LocalDateTime createdAt;
    private String professorName;
}
