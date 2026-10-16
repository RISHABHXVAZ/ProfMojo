package com.profmojo.unit;

import com.profmojo.models.AmenityRequest;
import com.profmojo.models.Staff;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.repositories.NotificationRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.services.AdminAmenityService;
import com.profmojo.services.impl.AdminAmenityServiceImpl;
import com.profmojo.services.impl.AmenityRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Amenity State Transition Policy & Invariant Tests")
class AmenityStateTransitionTest {

    @Mock private AmenityRequestRepository requestRepo;
    @Mock private StaffRepository staffRepo;
    @Mock private NotificationRepository notificationRepo;
    @Mock private AdminAmenityService adminAmenityService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private com.profmojo.metrics.AppMetricsService appMetricsService;

    @InjectMocks private AdminAmenityServiceImpl adminAmenityServiceReal;
    private AmenityRequestServiceImpl amenityRequestService;

    private Staff staff;
    private AmenityRequest request;

    @BeforeEach
    void setUp() {
        amenityRequestService = new AmenityRequestServiceImpl(
                requestRepo,
                staffRepo,
                notificationRepo,
                adminAmenityService,
                redisTemplate
        );

        staff = Staff.builder()
                .staffId("STAFF_01")
                .name("Alice Staff")
                .department("CSE")
                .available(true)
                .stars(5)
                .totalDeliveries(10)
                .build();

        request = AmenityRequest.builder()
                .id(100L)
                .professorId("PROF_01")
                .professorName("Dr. Turing")
                .department("CSE")
                .classRoom("Room 101")
                .items(List.of("Chalk", "Duster"))
                .status(RequestStatus.PENDING)
                .assignmentDeadline(LocalDateTime.now().plusMinutes(2))
                .build();
    }

    @Test
    @DisplayName("Legal transition: PENDING -> ASSIGNED succeeds")
    void transition_PendingToAssigned_Success() {
        when(requestRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(request));
        when(staffRepo.findByIdForUpdate("STAFF_01")).thenReturn(Optional.of(staff));
        when(requestRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(staffRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        AmenityRequest assigned = adminAmenityServiceReal.assignStaff(100L, "STAFF_01", "CSE");

        assertThat(assigned.getStatus()).isEqualTo(RequestStatus.ASSIGNED);
        assertThat(assigned.getAssignedStaff()).isEqualTo(staff);
        assertThat(assigned.getDeliveryConfirmationCode()).isNotNull().hasSize(4);
        assertThat(staff.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Legal transition: QUEUED -> ASSIGNED succeeds")
    void transition_QueuedToAssigned_Success() {
        request.setStatus(RequestStatus.QUEUED);
        when(requestRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(request));
        when(staffRepo.findByIdForUpdate("STAFF_01")).thenReturn(Optional.of(staff));
        when(requestRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(staffRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        AmenityRequest assigned = adminAmenityServiceReal.assignStaff(100L, "STAFF_01", "CSE");

        assertThat(assigned.getStatus()).isEqualTo(RequestStatus.ASSIGNED);
        assertThat(assigned.getAssignedStaff()).isEqualTo(staff);
        assertThat(staff.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Legal transition: ASSIGNED -> DELIVERED succeeds with valid code and updates stars")
    void transition_AssignedToDelivered_Success() {
        request.setStatus(RequestStatus.ASSIGNED);
        request.setAssignedStaff(staff);
        request.setDeliveryConfirmationCode("1234");
        request.setConfirmationCodeExpiry(LocalDateTime.now().plusHours(2));
        request.setSlaDeadline(LocalDateTime.now().plusMinutes(5));

        when(requestRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(request));
        when(staffRepo.findByIdForUpdate("STAFF_01")).thenReturn(Optional.of(staff));
        when(requestRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(staffRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = amenityRequestService.confirmDelivery(100L, "1234", staff);

        assertThat(result.get("slaBreached")).isEqualTo(false);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.DELIVERED);
        assertThat(request.getDeliveryConfirmationCode()).isNull();
        assertThat(request.getDeliveredAt()).isNotNull();
        assertThat(staff.isAvailable()).isTrue();
        assertThat(staff.getStars()).isEqualTo(6);
        assertThat(staff.getTotalDeliveries()).isEqualTo(11);
    }

    @Test
    @DisplayName("Illegal transition: DELIVERED cannot be assigned again")
    void illegalTransition_DeliveredToAssigned_Rejected() {
        request.setStatus(RequestStatus.DELIVERED);
        when(requestRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(request));

        assertThrows(RuntimeException.class, () ->
                adminAmenityServiceReal.assignStaff(100L, "STAFF_01", "CSE")
        );
        verify(requestRepo, never()).save(any());
        verify(staffRepo, never()).save(any());
    }

    @Test
    @DisplayName("Illegal transition: Duplicate delivery confirmation on DELIVERED request is rejected")
    void illegalTransition_DuplicateDeliveryConfirmation_Rejected() {
        request.setStatus(RequestStatus.DELIVERED);
        when(requestRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(request));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                amenityRequestService.confirmDelivery(100L, "1234", staff)
        );
        assertThat(ex.getMessage()).contains("already marked as delivered");
    }

    @Test
    @DisplayName("Illegal transition: Cannot deliver a QUEUED request")
    void illegalTransition_QueuedToDelivered_Rejected() {
        request.setStatus(RequestStatus.QUEUED);
        when(requestRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(request));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                amenityRequestService.confirmDelivery(100L, "1234", staff)
        );
        assertThat(ex.getMessage()).contains("Only assigned requests can be delivered");
    }

    @Test
    @DisplayName("Illegal transition: Cannot deliver a PENDING request")
    void illegalTransition_PendingToDelivered_Rejected() {
        request.setStatus(RequestStatus.PENDING);
        when(requestRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(request));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                amenityRequestService.confirmDelivery(100L, "1234", staff)
        );
        assertThat(ex.getMessage()).contains("Only assigned requests can be delivered");
    }

    @Test
    @DisplayName("Delivery rejected when attempted by unassigned staff")
    void delivery_WrongStaff_ThrowsAccessDenied() {
        request.setStatus(RequestStatus.ASSIGNED);
        request.setAssignedStaff(staff);
        request.setDeliveryConfirmationCode("1234");

        Staff differentStaff = Staff.builder().staffId("STAFF_OTHER").department("CSE").build();
        when(requestRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(request));

        assertThrows(AccessDeniedException.class, () ->
                amenityRequestService.confirmDelivery(100L, "1234", differentStaff)
        );
    }

    @Test
    @DisplayName("reRequestDueToSLABreach: Transitions to CANCELLED_SLA_BREACH, releases staff, creates new PENDING request")
    void reRequest_CancelsOldAndCreatesNew() {
        request.setStatus(RequestStatus.ASSIGNED);
        request.setAssignedStaff(staff);
        request.setDeliveryConfirmationCode("1234");
        staff.setAvailable(false);

        when(requestRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(request));
        when(staffRepo.findByIdForUpdate("STAFF_01")).thenReturn(Optional.of(staff));
        when(requestRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        AmenityRequest newReq = amenityRequestService.reRequestDueToSLABreach(100L, "PROF_01");

        assertThat(request.getStatus()).isEqualTo(RequestStatus.CANCELLED_SLA_BREACH);
        assertThat(request.getDeliveryConfirmationCode()).isNull();
        assertThat(staff.isAvailable()).isTrue();

        assertThat(newReq.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(newReq.getProfessorId()).isEqualTo("PROF_01");
        assertThat(newReq.getClassRoom()).isEqualTo("Room 101");
    }

    @Test
    @DisplayName("reRequestDueToSLABreach: Re-requesting an already DELIVERED request is rejected")
    void reRequest_DeliveredRequest_Rejected() {
        request.setStatus(RequestStatus.DELIVERED);
        when(requestRepo.findByIdForUpdate(100L)).thenReturn(Optional.of(request));

        assertThrows(IllegalStateException.class, () ->
                amenityRequestService.reRequestDueToSLABreach(100L, "PROF_01")
        );
    }
}
