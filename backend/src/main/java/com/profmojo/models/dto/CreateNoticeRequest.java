package com.profmojo.models.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateNoticeRequest {
    private String title;
    private String message;
    private List<String> classCodes; // resolved already (or select all)
}
