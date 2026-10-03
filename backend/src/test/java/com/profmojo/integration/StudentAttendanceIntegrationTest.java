package com.profmojo.integration;

import com.profmojo.models.Attendance;
import com.profmojo.models.ClassEnrollment;
import com.profmojo.models.ClassRoom;
import com.profmojo.models.Professor;
import com.profmojo.models.Student;
import com.profmojo.models.dto.StudentAttendanceSummaryDTO;
import com.profmojo.repositories.*;
import com.profmojo.services.AttendanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Student Attendance Aggregate Query Integration Tests")
class StudentAttendanceIntegrationTest extends BasePostgresContainerTest {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private AttendanceRepository attendanceRepo;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepo;

    @Autowired
    private ClassRoomRepository classRoomRepo;

    @Autowired
    private ProfessorRepository professorRepo;

    @Autowired
    private StudentRepository studentRepo;

    private Professor testProfessor;
    private Student studentA;
    private Student studentB;
    private Student studentZeroClasses;

    @BeforeEach
    void setUp() {
        attendanceRepo.deleteAll();
        classEnrollmentRepo.deleteAll();
        classRoomRepo.deleteAll();

        // 1. Create Professor
        testProfessor = Professor.builder()
                .profId("PROF_ATT_TEST")
                .name("Dr. Edsger Dijkstra")
                .department("CSE")
                .email("dijkstra@profmojo.edu")
                .password("hash")
                .build();
        professorRepo.save(testProfessor);

        // 2. Create Students
        studentA = Student.builder()
                .regNo("STU_ATT_A")
                .name("Alice Wonder")
                .email("alice@test.edu")
                .password("hash")
                .role("STUDENT")
                .build();
        studentRepo.save(studentA);

        studentB = Student.builder()
                .regNo("STU_ATT_B")
                .name("Bob Builder")
                .email("bob@test.edu")
                .password("hash")
                .role("STUDENT")
                .build();
        studentRepo.save(studentB);

        studentZeroClasses = Student.builder()
                .regNo("STU_ATT_ZERO")
                .name("Charlie Zero")
                .email("charlie@test.edu")
                .password("hash")
                .role("STUDENT")
                .build();
        studentRepo.save(studentZeroClasses);
    }

    private ClassRoom createClass(String classCode, String className) {
        ClassRoom cr = new ClassRoom();
        cr.setClassCode(classCode);
        cr.setClassName(className);
        cr.setProfessor(testProfessor);
        return classRoomRepo.save(cr);
    }

    private void enroll(Student student, ClassRoom classRoom) {
        ClassEnrollment ce = new ClassEnrollment();
        ce.setStudent(student);
        ce.setClassRoom(classRoom);
        ce.setClassCode(classRoom.getClassCode());
        classEnrollmentRepo.save(ce);
    }

    private void mark(String classCode, String regNo, LocalDate date, boolean present) {
        Attendance a = Attendance.builder()
                .classCode(classCode)
                .studentRegNo(regNo)
                .attendanceDate(date)
                .present(present)
                .build();
        attendanceRepo.save(a);
    }

    @Test
    @DisplayName("Student with zero enrolled classes returns empty list")
    void studentWithZeroEnrolledClasses_ReturnsEmptyList() {
        List<StudentAttendanceSummaryDTO> result = attendanceService.getMyAttendance(studentZeroClasses.getRegNo());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Student with one class and zero attendance records returns 0 lectures and 0% percentage")
    void studentWithOneClass_ZeroAttendanceRecords_ReturnsZeroStats() {
        ClassRoom cr = createClass("CS_ZERO", "Intro to CS");
        enroll(studentA, cr);

        List<StudentAttendanceSummaryDTO> result = attendanceService.getMyAttendance(studentA.getRegNo());

        assertNotNull(result);
        assertEquals(1, result.size());

        StudentAttendanceSummaryDTO summary = result.get(0);
        assertEquals("CS_ZERO", summary.getClassCode());
        assertEquals("Intro to CS", summary.getClassName());
        assertEquals("Dr. Edsger Dijkstra", summary.getProfessorName());
        assertEquals(0L, summary.getTotalLectures());
        assertEquals(0L, summary.getPresentCount());
        assertEquals(0.0, summary.getPercentage());
    }

    @Test
    @DisplayName("Student with multiple classes and mixed present/absent records computes correct stats")
    void studentWithMultipleClasses_MixedAttendance_CorrectStats() {
        ClassRoom cr1 = createClass("CS_ALGO", "Algorithms");
        ClassRoom cr2 = createClass("CS_OS", "Operating Systems");
        ClassRoom cr3 = createClass("CS_NET", "Computer Networks");

        enroll(studentA, cr1);
        enroll(studentA, cr2);
        enroll(studentA, cr3);

        LocalDate baseDate = LocalDate.of(2026, 9, 1);

        // Class 1: 4 distinct dates, 3 present, 1 absent -> 75.0%
        mark("CS_ALGO", studentA.getRegNo(), baseDate.plusDays(1), true);
        mark("CS_ALGO", studentA.getRegNo(), baseDate.plusDays(2), true);
        mark("CS_ALGO", studentA.getRegNo(), baseDate.plusDays(3), true);
        mark("CS_ALGO", studentA.getRegNo(), baseDate.plusDays(4), false);

        // Class 2: 2 distinct dates, 0 present, 2 absent -> 0.0%
        mark("CS_OS", studentA.getRegNo(), baseDate.plusDays(1), false);
        mark("CS_OS", studentA.getRegNo(), baseDate.plusDays(2), false);

        // Class 3: 0 lectures conducted -> 0.0%

        List<StudentAttendanceSummaryDTO> result = attendanceService.getMyAttendance(studentA.getRegNo());

        assertNotNull(result);
        assertEquals(3, result.size());

        // Verify Class 1
        StudentAttendanceSummaryDTO algo = result.stream()
                .filter(r -> r.getClassCode().equals("CS_ALGO"))
                .findFirst().orElseThrow();
        assertEquals(4L, algo.getTotalLectures());
        assertEquals(3L, algo.getPresentCount());
        assertEquals(75.0, algo.getPercentage());

        // Verify Class 2
        StudentAttendanceSummaryDTO os = result.stream()
                .filter(r -> r.getClassCode().equals("CS_OS"))
                .findFirst().orElseThrow();
        assertEquals(2L, os.getTotalLectures());
        assertEquals(0L, os.getPresentCount());
        assertEquals(0.0, os.getPercentage());

        // Verify Class 3
        StudentAttendanceSummaryDTO net = result.stream()
                .filter(r -> r.getClassCode().equals("CS_NET"))
                .findFirst().orElseThrow();
        assertEquals(0L, net.getTotalLectures());
        assertEquals(0L, net.getPresentCount());
        assertEquals(0.0, net.getPercentage());
    }

    @Test
    @DisplayName("Multiple students in same class: attendance is strictly isolated between students")
    void studentIsolation_MultipleStudentsInSameClass() {
        ClassRoom cr = createClass("CS_SHARED", "Distributed Systems");
        enroll(studentA, cr);
        enroll(studentB, cr);

        LocalDate d1 = LocalDate.of(2026, 9, 10);
        LocalDate d2 = LocalDate.of(2026, 9, 11);

        // Student A: present on both dates -> 2/2 = 100%
        mark("CS_SHARED", studentA.getRegNo(), d1, true);
        mark("CS_SHARED", studentA.getRegNo(), d2, true);

        // Student B: present on d1, absent on d2 -> 1/2 = 50%
        mark("CS_SHARED", studentB.getRegNo(), d1, true);
        mark("CS_SHARED", studentB.getRegNo(), d2, false);

        List<StudentAttendanceSummaryDTO> resA = attendanceService.getMyAttendance(studentA.getRegNo());
        List<StudentAttendanceSummaryDTO> resB = attendanceService.getMyAttendance(studentB.getRegNo());

        assertEquals(1, resA.size());
        assertEquals(2L, resA.get(0).getTotalLectures());
        assertEquals(2L, resA.get(0).getPresentCount());
        assertEquals(100.0, resA.get(0).getPercentage());

        assertEquals(1, resB.size());
        assertEquals(2L, resB.get(0).getTotalLectures());
        assertEquals(1L, resB.get(0).getPresentCount());
        assertEquals(50.0, resB.get(0).getPercentage());
    }
}
