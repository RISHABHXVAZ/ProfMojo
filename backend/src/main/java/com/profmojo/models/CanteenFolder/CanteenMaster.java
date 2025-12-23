package com.profmojo.models.CanteenFolder;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanteenMaster {

    @Id
    private String canteenId;

    private String canteenName;

    private String location;

    private boolean active = true;
}
