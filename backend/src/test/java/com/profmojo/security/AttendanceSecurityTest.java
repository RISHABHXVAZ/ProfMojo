package com.profmojo.security;

import com.profmojo.models.Attendance;
import com.profmojo.models.ClassRoom;
import com.profmojo.models.Professor;
import com.profmojo.models.dto.AttendanceStudentSummaryDTO;
import com.profmojo.models.dto.AttendanceSummaryDTO;
import com.profmojo.repositories.AttendanceRepository;
import com.profmojo.repositories.ClassEnrollmentRepository;
import com.profmojo.repositories.ClassRoomRepository;
import com.profmojo.repositories.StudentClassRepository;
import com.profmojo.services.impl.AttendanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 1 Security - Attendance Ownership & Enrollment Validation Tests")
class AttendanceSecurityTest {

    @Mock
    private AttendanceRepository attendanceRepo;

    @Mock
    private ClassRoomRepository classRoomRepo;

    @Mock
    private StudentClassRepository studentClassRepo;

    @Mock
    private ClassEnrollmentRepository classEnrollmentRepo;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private Professor ownerProf;
    private Professor attackerProf;
    private ClassRoom classRoom;

    @BeforeEach
    void setUp() {
        ownerProf = new Professor();
        ownerProf.setProfId("PROF_OWNER");
        ownerProf.setName("Prof Owner");

        attackerProf = new Professor();
        attackerProf.setProfId("PROF_ATTACKER");
        attackerProf.setName("Prof Attacker");

        classRoom = new ClassRoom();
        classRoom.setId(1L);
        classRoom.setClassCode("CSE-101");
        classRoom.setClassName("Computer Networks");
        classRoom.setProfessor(ownerProf);
    }

    @Test
    @DisplayName("markAttendance: Non-owner professor throws AccessDeniedException")
    void markAttendance_NonOwnerProfessor_ThrowsAccessDenied() {
        when(classRoomRepo.findByClassCode("CSE-101")).thenReturn(Optional.of(classRoom));

        assertThrows(AccessDeniedException.class, () ->
                attendanceService.markAttendance(attackerProf, "CSE-101", "STU001", true, LocalDate.now())
        );

        verify(attendanceRepo, never()).save(any());
    }

    @Test
    @DisplayName("markAttendance: Student not enrolled in class throws IllegalArgumentException")
    void markAttendance_StudentNotEnrolled_ThrowsIllegalArgument() {
        when(classRoomRepo.findByClassCode("CSE-101")).thenReturn(Optional.of(classRoom));
        when(classEnrollmentRepo.existsByClassCodeAndStudent_RegNo("CSE-101", "STU999")).thenReturn(false);
        when(studentClassRepo.existsByStudentRegNoAndClassCode("STU999", "CSE-101")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                attendanceService.markAttendance(ownerProf, "CSE-101", "STU999", true, LocalDate.now())
        );
        assertTrue(ex.getMessage().contains("not enrolled"));
        verify(attendanceRepo, never()).save(any());
    }

    @Test
    @DisplayName("markAttendance: Authorized owner with enrolled student successfully saves attendance")
    void markAttendance_AuthorizedOwnerAndEnrolledStudent_Success() {
        when(classRoomRepo.findByClassCode("CSE-101")).thenReturn(Optional.of(classRoom));
        when(classEnrollmentRepo.existsByClassCodeAndStudent_RegNo("CSE-101", "STU001")).thenReturn(true);
        when(attendanceRepo.findByClassCodeAndStudentRegNoAndAttendanceDate("CSE-101", "STU001", LocalDate.now()))
                .thenReturn(Optional.empty());

        attendanceService.markAttendance(ownerProf, "CSE-101", "STU001", true, LocalDate.now());

        verify(attendanceRepo, times(1)).save(any(Attendance.class));
    }

    @Test
    @DisplayName("getAttendanceForClassToday: Non-owner professor throws AccessDeniedException")
    void getAttendanceForClassToday_NonOwnerProfessor_ThrowsAccessDenied() {
        when(classRoomRepo.findByClassCode("CSE-101")).thenReturn(Optional.of(classRoom));

        assertThrows(AccessDeniedException.class, () ->
                attendanceService.getAttendanceForClassToday("CSE-101", attackerProf)
        );
    }

    @Test
    @DisplayName("getAttendanceForClassToday: Authorized owner successfully retrieves attendance")
    void getAttendanceForClassToday_AuthorizedOwner_Success() {
        when(classRoomRepo.findByClassCode("CSE-101")).thenReturn(Optional.of(classRoom));
        when(attendanceRepo.findByClassCodeAndAttendanceDate(eq("CSE-101"), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        List<Attendance> result = attendanceService.getAttendanceForClassToday("CSE-101", ownerProf);
        assertNotNull(result);
    }

    @Test
    @DisplayName("getAttendanceForClassByDate: Non-owner professor throws AccessDeniedException")
    void getAttendanceForClassByDate_NonOwnerProfessor_ThrowsAccessDenied() {
        when(classRoomRepo.findByClassCode("CSE-101")).thenReturn(Optional.of(classRoom));

        assertThrows(AccessDeniedException.class, () ->
                attendanceService.getAttendanceForClassByDate("CSE-101", LocalDate.now(), attackerProf)
        );
    }

    @Test
    @DisplayName("getStudentAttendanceHistory: Non-owner professor throws AccessDeniedException")
    void getStudentAttendanceHistory_NonOwnerProfessor_ThrowsAccessDenied() {
        when(classRoomRepo.findByClassCode("CSE-101")).thenReturn(Optional.of(classRoom));

        assertThrows(AccessDeniedException.class, () ->
                attendanceService.getStudentAttendanceHistory("CSE-101", "STU001", attackerProf)
        );
    }

    @Test
    @DisplayName("getStudentAttendanceHistory: Non-enrolled student throws IllegalArgumentException")
    void getStudentAttendanceHistory_NonEnrolledStudent_ThrowsIllegalArgument() {
        when(classRoomRepo.findByClassCode("CSE-101")).thenReturn(Optional.of(classRoom));
        when(classEnrollmentRepo.existsByClassCodeAndStudent_RegNo("CSE-101", "STU999")).thenReturn(false);
        when(studentClassRepo.existsByStudentRegNoAndClassCode("STU999", "CSE-101")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () ->
                attendanceService.getStudentAttendanceHistory("CSE-101", "STU999", ownerProf)
        );
    }

    @Test
    @DisplayName("getAttendanceSummary: Non-owner professor throws AccessDeniedException")
    void getAttendanceSummary_NonOwnerProfessor_ThrowsAccessDenied() {
        when(classRoomRepo.findByClassCode("CSE-101")).thenReturn(Optional.of(classRoom));

        assertThrows(AccessDeniedException.class, () ->
                attendanceService.getAttendanceSummary("CSE-101", attackerProf)
        );
    }

    @Test
    @DisplayName("getAttendanceSummary: Authorized owner successfully retrieves summary")
    void getAttendanceSummary_AuthorizedOwner_Success() {
        when(classRoomRepo.findByClassCode("CSE-101")).thenReturn(Optional.of(classRoom));
        when(attendanceRepo.countTotalLectures("CSE-101")).thenReturn(10L);
        when(attendanceRepo.getAverageAttendance("CSE-101")).thenReturn(85.5);
        when(attendanceRepo.findLowAttendanceStudents("CSE-101")).thenReturn(Collections.emptyList());

        AttendanceSummaryDTO summary = attendanceService.getAttendanceSummary("CSE-101", ownerProf);
        assertNotNull(summary);
        assertEquals(10L, summary.getTotalLectures());
        assertEquals(85.5, summary.getAverageAttendance());
    }

    @Test
    @DisplayName("getStudentAttendanceSummary: Non-owner professor throws AccessDeniedException")
    void getStudentAttendanceSummary_NonOwnerProfessor_ThrowsAccessDenied() {
        when(classRoomRepo.findByClassCode("CSE-101")).thenReturn(Optional.of(classRoom));

        assertThrows(AccessDeniedException.class, () ->
                attendanceService.getStudentAttendanceSummary("CSE-101", attackerProf)
        );
    }

    @Test
    @DisplayName("getStudentAttendanceSummary: Authorized owner successfully retrieves student summary")
    void getStudentAttendanceSummary_AuthorizedOwner_Success() {
        when(classRoomRepo.findByClassCode("CSE-101")).thenReturn(Optional.of(classRoom));
        when(attendanceRepo.getStudentAttendanceSummary("CSE-101")).thenReturn(Collections.emptyList());

        List<AttendanceStudentSummaryDTO> result = attendanceService.getStudentAttendanceSummary("CSE-101", ownerProf);
        assertNotNull(result);
    }

    @Test
    @DisplayName("markAttendance: Null professor throws AccessDeniedException")
    void markAttendance_NullProfessor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () ->
                attendanceService.markAttendance(null, "CSE-101", "STU001", true, LocalDate.now())
        );
        verify(attendanceRepo, never()).save(any());
    }

    @Test
    @DisplayName("getAttendanceForClassToday: Null professor throws AccessDeniedException")
    void getAttendanceForClassToday_NullProfessor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () ->
                attendanceService.getAttendanceForClassToday("CSE-101", null)
        );
    }

    @Test
    @DisplayName("getAttendanceForClassByDate: Null professor throws AccessDeniedException")
    void getAttendanceForClassByDate_NullProfessor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () ->
                attendanceService.getAttendanceForClassByDate("CSE-101", LocalDate.now(), null)
        );
    }

    @Test
    @DisplayName("getStudentAttendanceHistory: Null professor throws AccessDeniedException")
    void getStudentAttendanceHistory_NullProfessor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () ->
                attendanceService.getStudentAttendanceHistory("CSE-101", "STU001", null)
        );
    }

    @Test
    @DisplayName("getAttendanceSummary: Null professor throws AccessDeniedException")
    void getAttendanceSummary_NullProfessor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () ->
                attendanceService.getAttendanceSummary("CSE-101", null)
        );
    }

    @Test
    @DisplayName("getStudentAttendanceSummary: Null professor throws AccessDeniedException")
    void getStudentAttendanceSummary_NullProfessor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () ->
                attendanceService.getStudentAttendanceSummary("CSE-101", null)
        );
    }
}
