package com.profmojo.unit;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Notification;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAmenityService Unit Tests")
class AdminAmenityServiceTest {

    @Mock
    private AmenityRequestRepository requestRepo;

    @Mock
    private StaffRepository staffRepo;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private AdminAmenityServiceImpl amenityService;

    private AmenityRequest testRequest;
    private Staff availableStaff;
    private Staff busyStaff;

    @BeforeEach
    void setUp() {
        testRequest = AmenityRequest.builder()
                .id(100L)
                .professorId("PROF01")
                .classRoom("LH-201")
                .department("CSE")
                .status(RequestStatus.PENDING)
                .build();

        availableStaff = TestDataFactory.createStaff("STAFF01", "Ramesh Kumar", "CSE", "ramesh@test.edu", true, true);
        busyStaff = TestDataFactory.createStaff("STAFF02", "Suresh Patel", "CSE", "suresh@test.edu", false, true);
    }

    @Test
    @DisplayName("assignStaff: Successfully assigns available staff and updates status to ASSIGNED")
    void assignStaff_AvailableStaff_Success() {
        when(requestRepo.findById(100L)).thenReturn(Optional.of(testRequest));
        when(staffRepo.findById("STAFF01")).thenReturn(Optional.of(availableStaff));
        when(staffRepo.save(any(Staff.class))).thenAnswer(i -> i.getArgument(0));
        when(requestRepo.save(any(AmenityRequest.class))).thenAnswer(i -> i.getArgument(0));

        AmenityRequest result = amenityService.assignStaff(100L, "STAFF01");

        assertNotNull(result);
        assertEquals(RequestStatus.ASSIGNED, result.getStatus());
        assertEquals("STAFF01", result.getAssignedStaff().getStaffId());
        assertFalse(availableStaff.isAvailable(), "Staff should be marked unavailable after assignment");
        assertNotNull(result.getAssignedAt());
        assertNotNull(result.getSlaDeadline());

        verify(staffRepo).save(availableStaff);
        verify(requestRepo).save(result);
    }

    @Test
    @DisplayName("assignStaff: Unavailable staff throws RuntimeException")
    void assignStaff_UnavailableStaff_ThrowsException() {
        when(requestRepo.findById(100L)).thenReturn(Optional.of(testRequest));
        when(staffRepo.findById("STAFF02")).thenReturn(Optional.of(busyStaff));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> amenityService.assignStaff(100L, "STAFF02"));
        assertEquals("Staff is not available", ex.getMessage());

        verify(requestRepo, never()).save(any());
    }

    @Test
    @DisplayName("addToQueue: Pending request successfully transitions to QUEUED and sends notification")
    void addToQueue_PendingRequest_QueuesAndNotifies() {
        when(requestRepo.findById(100L)).thenReturn(Optional.of(testRequest));
        when(requestRepo.save(any(AmenityRequest.class))).thenAnswer(i -> i.getArgument(0));

        AmenityRequest queued = amenityService.addToQueue(100L);

        assertEquals(RequestStatus.QUEUED, queued.getStatus());
        assertNotNull(queued.getDeliveryConfirmationCode());
        assertNotNull(queued.getConfirmationCodeExpiry());

        ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notifCaptor.capture());
        Notification notification = notifCaptor.getValue();
        assertEquals("PROF01", notification.getRecipientId());
        assertEquals("PROFESSOR", notification.getRecipientRole());
        assertEquals("REQUEST_QUEUED", notification.getEventType());
    }

    @Test
    @DisplayName("addToQueue: Non-pending request throws RuntimeException")
    void addToQueue_NonPendingRequest_ThrowsException() {
        testRequest.setStatus(RequestStatus.ASSIGNED);
        when(requestRepo.findById(100L)).thenReturn(Optional.of(testRequest));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> amenityService.addToQueue(100L));
        assertEquals("Only pending requests can be queued", ex.getMessage());

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("tryAssignQueuedRequest: Automatically assigns oldest queued request to available staff in FIFO order")
    void tryAssignQueuedRequest_AssignsOldestQueuedRequest() {
        AmenityRequest queuedRequest = AmenityRequest.builder()
                .id(101L)
                .professorId("PROF02")
                .classRoom("LH-102")
                .department("CSE")
                .status(RequestStatus.QUEUED)
                .build();

        when(requestRepo.findFirstByStatusAndDepartmentOrderByCreatedAtAsc(RequestStatus.QUEUED, "CSE"))
                .thenReturn(Optional.of(queuedRequest));

        amenityService.tryAssignQueuedRequest(availableStaff);

        assertFalse(availableStaff.isAvailable(), "Staff should become unavailable");
        assertEquals(RequestStatus.ASSIGNED, queuedRequest.getStatus());
        assertEquals(availableStaff, queuedRequest.getAssignedStaff());

        verify(staffRepo).save(availableStaff);
        verify(requestRepo).save(queuedRequest);
    }
}
