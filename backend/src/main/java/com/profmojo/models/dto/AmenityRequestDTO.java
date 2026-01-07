package com.profmojo.models.dto;

import lombok.Data;

import java.util.List;

@Data
public class AmenityRequestDTO {
    private String professorName;
    private String classRoom;
    private String department;
    private List<String> items;
}

