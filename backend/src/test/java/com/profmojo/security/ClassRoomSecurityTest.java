package com.profmojo.security;

import com.profmojo.models.ClassEnrollment;
import com.profmojo.models.ClassRoom;
import com.profmojo.models.Professor;
import com.profmojo.models.Student;
import com.profmojo.repositories.ClassEnrollmentRepository;
import com.profmojo.repositories.ClassRoomRepository;
import com.profmojo.services.impl.ClassRoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 1 Security - ClassRoom Student Listing Ownership Tests")
class ClassRoomSecurityTest {

    @Mock
    private ClassRoomRepository classRoomRepo;

    @Mock
    private ClassEnrollmentRepository classEnrollmentRepo;

    @Mock
    private com.profmojo.repositories.AttendanceRepository attendanceRepo;

    @InjectMocks
    private ClassRoomServiceImpl classRoomService;

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
        classRoom.setId(10L);
        classRoom.setClassCode("CSE-202");
        classRoom.setClassName("Operating Systems");
        classRoom.setProfessor(ownerProf);
    }

    @Test
    @DisplayName("getStudentsOfClass: Non-owner professor throws AccessDeniedException")
    void getStudentsOfClass_NonOwnerProfessor_ThrowsAccessDenied() {
        when(classRoomRepo.findByClassCode("CSE-202")).thenReturn(Optional.of(classRoom));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                classRoomService.getStudentsOfClass("CSE-202", attackerProf)
        );
        assertTrue(ex.getMessage().contains("do not own"));
        verify(classEnrollmentRepo, never()).findByClassCode(any());
    }

    @Test
    @DisplayName("getStudentsOfClass: Class does not exist throws IllegalArgumentException")
    void getStudentsOfClass_ClassNotFound_ThrowsIllegalArgument() {
        when(classRoomRepo.findByClassCode("CSE-NONEXIST")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                classRoomService.getStudentsOfClass("CSE-NONEXIST", ownerProf)
        );
    }

    @Test
    @DisplayName("getStudentsOfClass: Authorized owner successfully retrieves enrolled student roster")
    void getStudentsOfClass_AuthorizedOwner_Success() {
        when(classRoomRepo.findByClassCode("CSE-202")).thenReturn(Optional.of(classRoom));

        Student student = new Student();
        student.setRegNo("STU001");
        student.setName("Alice");

        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setId(1L);
        enrollment.setClassCode("CSE-202");
        enrollment.setClassRoom(classRoom);
        enrollment.setStudent(student);

        when(classEnrollmentRepo.findByClassCode("CSE-202")).thenReturn(List.of(enrollment));

        List<ClassEnrollment> results = classRoomService.getStudentsOfClass("CSE-202", ownerProf);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("STU001", results.get(0).getStudent().getRegNo());
    }

    @Test
    @DisplayName("getStudentsOfClass: Null professor throws AccessDeniedException")
    void getStudentsOfClass_NullProfessor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () ->
                classRoomService.getStudentsOfClass("CSE-202", null)
        );
        verify(classEnrollmentRepo, never()).findByClassCode(any());
    }

    @Test
    @DisplayName("deleteClass: Null professor throws AccessDeniedException")
    void deleteClass_NullProfessor_ThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, () ->
                classRoomService.deleteClass("CSE-202", null)
        );
        verify(classRoomRepo, never()).delete(any());
    }

    @Test
    @DisplayName("deleteClass: Non-owner professor throws AccessDeniedException")
    void deleteClass_NonOwnerProfessor_ThrowsAccessDenied() {
        when(classRoomRepo.findByClassCode("CSE-202")).thenReturn(Optional.of(classRoom));

        assertThrows(AccessDeniedException.class, () ->
                classRoomService.deleteClass("CSE-202", attackerProf)
        );
        verify(classRoomRepo, never()).delete(any());
    }

    @Test
    @DisplayName("deleteClass: Authorized owner successfully deletes class and cascades")
    void deleteClass_AuthorizedOwner_Success() {
        when(classRoomRepo.findByClassCode("CSE-202")).thenReturn(Optional.of(classRoom));

        classRoomService.deleteClass("CSE-202", ownerProf);

        verify(attendanceRepo, times(1)).deleteByClassCode("CSE-202");
        verify(classEnrollmentRepo, times(1)).deleteByClassCode("CSE-202");
        verify(classRoomRepo, times(1)).delete(classRoom);
    }
}
