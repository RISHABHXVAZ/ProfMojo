package com.profmojo.unit;

import com.profmojo.models.dto.StudentAttendanceSummaryDTO;
import com.profmojo.repositories.AttendanceRepository;
import com.profmojo.repositories.ClassRoomRepository;
import com.profmojo.repositories.StudentClassRepository;
import com.profmojo.services.impl.AttendanceServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceService Unit & Regression Tests")
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepo;

    @Mock
    private ClassRoomRepository classRoomRepo;

    @Mock
    private StudentClassRepository studentClassRepo;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    @Test
    @DisplayName("getMyAttendance: Delegates directly to single repository aggregate query")
    void getMyAttendance_DelegatesToRepositoryAggregate() {
        StudentAttendanceSummaryDTO summary = new StudentAttendanceSummaryDTO(
                "CS101", "Algorithms", "Dr. Turing", 10, 8
        );
        when(attendanceRepo.getStudentAttendance("STU001")).thenReturn(List.of(summary));

        List<StudentAttendanceSummaryDTO> result = attendanceService.getMyAttendance("STU001");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CS101", result.get(0).getClassCode());
        assertEquals(80.0, result.get(0).getPercentage());

        verify(attendanceRepo, times(1)).getStudentAttendance("STU001");
        verifyNoInteractions(classRoomRepo);
    }

    @Test
    @DisplayName("getStudentAttendance: Regression test - delegates to single aggregate query without per-class loop")
    void getStudentAttendance_RegressionTest_NoPerClassLoop() {
        StudentAttendanceSummaryDTO summary = new StudentAttendanceSummaryDTO(
                "CS102", "Operating Systems", "Dr. Ritchie", 5, 4
        );
        when(attendanceRepo.getStudentAttendance("STU002")).thenReturn(List.of(summary));

        List<StudentAttendanceSummaryDTO> result = attendanceService.getStudentAttendance("STU002");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CS102", result.get(0).getClassCode());
        assertEquals(80.0, result.get(0).getPercentage());

        // Verifies no per-class loops or secondary repository calls
        verify(attendanceRepo, times(1)).getStudentAttendance("STU002");
        verifyNoInteractions(classRoomRepo);
    }

    @Test
    @DisplayName("getStudentDashboard: Regression test - delegates to single aggregate query without per-class loop")
    void getStudentDashboard_RegressionTest_NoPerClassLoop() {
        StudentAttendanceSummaryDTO summary = new StudentAttendanceSummaryDTO(
                "CS103", "Databases", "Dr. Codd", 12, 12
        );
        when(attendanceRepo.getStudentAttendance("STU003")).thenReturn(List.of(summary));

        List<StudentAttendanceSummaryDTO> result = attendanceService.getStudentDashboard("STU003");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CS103", result.get(0).getClassCode());
        assertEquals(100.0, result.get(0).getPercentage());

        verify(attendanceRepo, times(1)).getStudentAttendance("STU003");
        verifyNoInteractions(classRoomRepo);
    }
}
