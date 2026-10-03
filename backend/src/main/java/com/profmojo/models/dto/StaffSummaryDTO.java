package com.profmojo.models.dto;

import com.profmojo.models.Staff;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffSummaryDTO {
    private String staffId;
    private String name;
    private String department;
    private String contactNo;
    private boolean available;
    private String email;
    private boolean online;
    private Integer stars;
    private Integer totalDeliveries;
    private String role;

    public static StaffSummaryDTO fromEntity(Staff staff) {
        if (staff == null) {
            return null;
        }
        return StaffSummaryDTO.builder()
                .staffId(staff.getStaffId())
                .name(staff.getName())
                .department(staff.getDepartment())
                .contactNo(staff.getContactNo())
                .available(staff.isAvailable())
                .email(staff.getEmail())
                .online(staff.isOnline())
                .stars(staff.getStars())
                .totalDeliveries(staff.getTotalDeliveries())
                .role(staff.getRole())
                .build();
    }
}
