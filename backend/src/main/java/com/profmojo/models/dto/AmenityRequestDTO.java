package com.profmojo.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class AmenityRequestDTO {
    private String professorName;

    @JsonProperty("classroom")
    private String classRoom;

    private String department;
    private List<String> items;
}

