package com.profmojo.models.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StaffLoginResponse {
    private String token;
    private String staffId;
    private String department;
}
