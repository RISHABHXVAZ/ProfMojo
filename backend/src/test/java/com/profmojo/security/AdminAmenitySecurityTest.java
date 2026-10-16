package com.profmojo.security;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.services.impl.AdminAmenityServiceImpl;
import com.profmojo.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 1 Security - Admin Amenity Department Isolation Tests")
class AdminAmenitySecurityTest {

    @Mock
    private AmenityRequestRepository requestRepo;

    @Mock
    private StaffRepository staffRepo;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private com.profmojo.metrics.AppMetricsService appMetricsService;

    @InjectMocks
    private AdminAmenityServiceImpl amenityService;

    private AmenityRequest cseRequest;
    private AmenityRequest eceRequest;
    private Staff cseStaff;
    private Staff eceStaff;

    @BeforeEach
    void setUp() {
        cseRequest = AmenityRequest.builder()
                .id(1L)
                .professorId("PROF01")
                .classRoom("LH-101")
                .department("CSE")
                .status(RequestStatus.PENDING)
                .build();

        eceRequest = AmenityRequest.builder()
                .id(2L)
                .professorId("PROF02")
                .classRoom("LH-201")
                .department("ECE")
                .status(RequestStatus.PENDING)
                .build();

        cseStaff = TestDataFactory.createStaff("STAFF_CSE", "John CSE", "CSE", "john@test.edu", true, true);
        eceStaff = TestDataFactory.createStaff("STAFF_ECE", "Mike ECE", "ECE", "mike@test.edu", true, true);
    }

    @Test
    @DisplayName("assignStaff: Admin attempting to assign request of another department throws AccessDeniedException")
    void assignStaff_CrossDepartmentRequest_ThrowsAccessDenied() {
        when(requestRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(eceRequest));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                amenityService.assignStaff(2L, "STAFF_CSE", "CSE")
        );
        assertTrue(ex.getMessage().contains("does not belong to your department"));
        verify(requestRepo, never()).save(any());
        verify(staffRepo, never()).save(any());
    }

    @Test
    @DisplayName("assignStaff: Admin attempting to assign staff from another department throws AccessDeniedException")
    void assignStaff_CrossDepartmentStaff_ThrowsAccessDenied() {
        when(requestRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(cseRequest));
        when(staffRepo.findByIdForUpdate("STAFF_ECE")).thenReturn(Optional.of(eceStaff));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                amenityService.assignStaff(1L, "STAFF_ECE", "CSE")
        );
        assertTrue(ex.getMessage().contains("Staff does not belong to your department"));
        verify(requestRepo, never()).save(any());
        verify(staffRepo, never()).save(any());
    }

    @Test
    @DisplayName("assignStaff: Staff and request departments mismatch throws AccessDeniedException")
    void assignStaff_StaffRequestDeptMismatch_ThrowsAccessDenied() {
        when(requestRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(cseRequest));
        when(staffRepo.findByIdForUpdate("STAFF_ECE")).thenReturn(Optional.of(eceStaff));

        assertThrows(AccessDeniedException.class, () ->
                amenityService.assignStaff(1L, "STAFF_ECE", null)
        );
        verify(requestRepo, never()).save(any());
    }

    @Test
    @DisplayName("assignStaff: Attempting to assign an already DELIVERED request throws RuntimeException")
    void assignStaff_InvalidStateDelivered_ThrowsException() {
        cseRequest.setStatus(RequestStatus.DELIVERED);
        when(requestRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(cseRequest));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                amenityService.assignStaff(1L, "STAFF_CSE", "CSE")
        );
        assertTrue(ex.getMessage().contains("Only pending or queued requests can be assigned"));
        verify(requestRepo, never()).save(any());
    }

    @Test
    @DisplayName("assignStaff: Same-department admin, request, and staff with PENDING status succeeds")
    void assignStaff_SameDepartmentAndPending_Success() {
        when(requestRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(cseRequest));
        when(staffRepo.findByIdForUpdate("STAFF_CSE")).thenReturn(Optional.of(cseStaff));
        when(staffRepo.save(any(Staff.class))).thenAnswer(i -> i.getArgument(0));
        when(requestRepo.save(any(AmenityRequest.class))).thenAnswer(i -> i.getArgument(0));

        AmenityRequest result = amenityService.assignStaff(1L, "STAFF_CSE", "CSE");

        assertNotNull(result);
        assertEquals(RequestStatus.ASSIGNED, result.getStatus());
        assertEquals(cseStaff, result.getAssignedStaff());
        assertFalse(cseStaff.isAvailable());
        verify(requestRepo, times(1)).save(result);
        verify(staffRepo, times(1)).save(cseStaff);
    }

    @Test
    @DisplayName("addToQueue: Admin attempting to queue request of another department throws AccessDeniedException")
    void addToQueue_CrossDepartmentRequest_ThrowsAccessDenied() {
        when(requestRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(eceRequest));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                amenityService.addToQueue(2L, "CSE")
        );
        assertTrue(ex.getMessage().contains("does not belong to your department"));
        verify(requestRepo, never()).save(any());
    }

    @Test
    @DisplayName("addToQueue: Attempting to queue non-pending request throws RuntimeException")
    void addToQueue_NonPendingRequest_ThrowsException() {
        cseRequest.setStatus(RequestStatus.ASSIGNED);
        when(requestRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(cseRequest));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                amenityService.addToQueue(1L, "CSE")
        );
        assertTrue(ex.getMessage().contains("Only pending requests can be queued"));
        verify(requestRepo, never()).save(any());
    }

    @Test
    @DisplayName("addToQueue: Same-department pending request queues successfully")
    void addToQueue_SameDepartmentPending_Success() {
        when(requestRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(cseRequest));
        when(requestRepo.save(any(AmenityRequest.class))).thenAnswer(i -> i.getArgument(0));

        AmenityRequest result = amenityService.addToQueue(1L, "CSE");

        assertNotNull(result);
        assertEquals(RequestStatus.QUEUED, result.getStatus());
        verify(requestRepo, times(1)).save(result);
    }
}
