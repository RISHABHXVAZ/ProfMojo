package com.profmojo.models.CanteenFolder;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Canteen {

    @Id
    private String canteenId;   // login id

    private String canteenName;

    private String password;

    private String contactNo;

    private Boolean active = true;

    private String role = "CANTEEN";
}
