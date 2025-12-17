package com.profmojo.models.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class StudentClassDTO {
    private String classCode;
    private String className;
    private String professorName;
    private long totalLectures;
    private long presentCount;

    public double getPercentage() {
        return totalLectures == 0 ? 0.0 :
                (presentCount * 100.0) / totalLectures;
    }
}
