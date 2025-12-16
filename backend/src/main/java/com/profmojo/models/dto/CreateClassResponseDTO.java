package com.profmojo.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateClassResponseDTO {
    private String classCode;
    private String className;
}
