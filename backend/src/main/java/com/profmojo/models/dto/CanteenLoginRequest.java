package com.profmojo.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CanteenLoginRequest {
    private String canteenId;
    private String password;
}

