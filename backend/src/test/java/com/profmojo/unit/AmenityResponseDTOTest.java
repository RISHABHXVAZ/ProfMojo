package com.profmojo.unit;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.dto.AmenityResponseDTO;
import com.profmojo.models.dto.StaffSummaryDTO;
import com.profmojo.models.enums.RequestStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AmenityResponseDTO & StaffSummaryDTO Mapping Tests")
class AmenityResponseDTOTest {

    @Test
    @DisplayName("fromEntity returns null when entity is null")
    void fromEntity_NullEntity_ReturnsNull() {
        assertNull(AmenityResponseDTO.fromEntity(null));
        assertNull(StaffSummaryDTO.fromEntity(null));
    }

    @Test
    @DisplayName("fromEntity correctly maps all fields with staff and items")
    void fromEntity_CompleteEntity_MapsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        Staff staff = Staff.builder()
                .staffId("STF001")
                .name("Ramesh Kumar")
                .department("CSE")
                .contactNo("9876543210")
                .available(true)
                .email("ramesh@test.edu")
                .password("super_secret_hash")
                .online(true)
                .stars(5)
                .totalDeliveries(42)
                .role("STAFF")
                .build();

        AmenityRequest entity = AmenityRequest.builder()
                .id(101L)
                .professorId("PROF01")
                .professorName("Dr. Alan Turing")
                .classRoom("LH-101")
                .department("CSE")
                .items(List.of("Chalk", "Duster", "HDMI Cable"))
                .status(RequestStatus.ASSIGNED)
                .assignedStaff(staff)
                .deliveryConfirmationCode("4819")
                .confirmationCodeExpiry(now.plusHours(2))
                .assignedAt(now)
                .deliveredAt(null)
                .slaDeadline(now.plusMinutes(5))
                .deliveryDeadline(now.plusMinutes(5))
                .slaBreached(false)
                .deliverySlaBreached(false)
                .createdAt(now.minusMinutes(1))
                .build();

        AmenityResponseDTO dto = AmenityResponseDTO.fromEntity(entity);

        assertNotNull(dto);
        assertEquals(101L, dto.getId());
        assertEquals("PROF01", dto.getProfessorId());
        assertEquals("Dr. Alan Turing", dto.getProfessorName());
        assertEquals("LH-101", dto.getClassRoom());
        assertEquals("CSE", dto.getDepartment());
        assertEquals(List.of("Chalk", "Duster", "HDMI Cable"), dto.getItems());
        assertEquals(RequestStatus.ASSIGNED, dto.getStatus());
        assertEquals("4819", dto.getDeliveryConfirmationCode());
        assertEquals(now.plusHours(2), dto.getConfirmationCodeExpiry());
        assertEquals(now, dto.getAssignedAt());
        assertNull(dto.getDeliveredAt());
        assertEquals(now.plusMinutes(5), dto.getSlaDeadline());
        assertEquals(now.plusMinutes(5), dto.getDeliveryDeadline());
        assertFalse(dto.isSlaBreached());
        assertEquals(Boolean.FALSE, dto.getDeliverySlaBreached());
        assertEquals(now.minusMinutes(1), dto.getCreatedAt());

        // Verify Staff Summary DTO
        StaffSummaryDTO staffDto = dto.getAssignedStaff();
        assertNotNull(staffDto);
        assertEquals("STF001", staffDto.getStaffId());
        assertEquals("Ramesh Kumar", staffDto.getName());
        assertEquals("CSE", staffDto.getDepartment());
        assertEquals("9876543210", staffDto.getContactNo());
        assertTrue(staffDto.isAvailable());
        assertEquals("ramesh@test.edu", staffDto.getEmail());
        assertTrue(staffDto.isOnline());
        assertEquals(5, staffDto.getStars());
        assertEquals(42, staffDto.getTotalDeliveries());
        assertEquals("STAFF", staffDto.getRole());
    }

    @Test
    @DisplayName("fromEntity correctly handles null assignedStaff and empty items")
    void fromEntity_UnassignedAndEmptyItems_MapsSafely() {
        AmenityRequest entity = AmenityRequest.builder()
                .id(102L)
                .professorId("PROF02")
                .professorName("Dr. Grace Hopper")
                .classRoom("LH-202")
                .department("CSE")
                .items(Collections.emptyList())
                .status(RequestStatus.PENDING)
                .assignedStaff(null)
                .createdAt(LocalDateTime.now())
                .build();

        AmenityResponseDTO dto = AmenityResponseDTO.fromEntity(entity);

        assertNotNull(dto);
        assertEquals(102L, dto.getId());
        assertNull(dto.getAssignedStaff());
        assertNotNull(dto.getItems());
        assertTrue(dto.getItems().isEmpty());
    }

    @Test
    @DisplayName("fromEntity handles null items safely without NullPointerException")
    void fromEntity_NullItems_ReturnsNullOrEmpty() {
        AmenityRequest entity = AmenityRequest.builder()
                .id(103L)
                .professorId("PROF03")
                .department("ECE")
                .items(null)
                .status(RequestStatus.QUEUED)
                .build();

        AmenityResponseDTO dto = AmenityResponseDTO.fromEntity(entity);

        assertNotNull(dto);
        assertNotNull(dto.getItems());
        assertTrue(dto.getItems().isEmpty());
    }
}
