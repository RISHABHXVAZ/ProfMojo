package com.profmojo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.profmojo.integration.BasePostgresContainerTest;
import com.profmojo.models.ClassEnrollment;
import com.profmojo.models.ClassRoom;
import com.profmojo.models.Professor;
import com.profmojo.models.Student;
import com.profmojo.models.dto.AttendanceRequest;
import com.profmojo.models.dto.CreateNoticeRequest;
import com.profmojo.repositories.*;
import com.profmojo.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 1 Security - Professor HTTP-Layer Authorization Integration Tests")
class ProfessorAuthorizationHttpTest extends BasePostgresContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private ProfessorRepository professorRepo;

    @Autowired
    private StudentRepository studentRepo;

    @Autowired
    private ClassRoomRepository classRoomRepo;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepo;

    @Autowired
    private AttendanceRepository attendanceRepo;

    @Autowired
    private NoticeRepository noticeRepo;

    @Autowired
    private NoticeClassMappingRepository noticeMappingRepo;

    private Professor ownerProf;
    private Professor attackerProf;
    private Student student;
    private ClassRoom classRoom;
    private String ownerToken;
    private String attackerToken;

    @BeforeEach
    void setUp() {
        attendanceRepo.deleteAll();
        noticeMappingRepo.deleteAll();
        noticeRepo.deleteAll();
        classEnrollmentRepo.deleteAll();
        classRoomRepo.deleteAll();
        studentRepo.deleteAll();
        professorRepo.deleteAll();

        // 1. Create Professors
        ownerProf = Professor.builder()
                .profId("PROF_HTTP_OWNER")
                .name("Dr. Alice Owner")
                .email("alice.owner@profmojo.edu")
                .department("CSE")
                .password("hash")
                .build();
        professorRepo.save(ownerProf);

        attackerProf = Professor.builder()
                .profId("PROF_HTTP_ATTACKER")
                .name("Dr. Bob Attacker")
                .email("bob.attacker@profmojo.edu")
                .department("ECE")
                .password("hash")
                .build();
        professorRepo.save(attackerProf);

        // 2. Create Student
        student = Student.builder()
                .regNo("STU_HTTP_01")
                .name("Charlie Student")
                .email("charlie@test.edu")
                .role("STUDENT")
                .password("hash")
                .build();
        studentRepo.save(student);

        // 3. Create Class owned by ownerProf
        classRoom = new ClassRoom();
        classRoom.setClassCode("CSE-HTTP-101");
        classRoom.setClassName("Distributed Systems");
        classRoom.setProfessor(ownerProf);
        classRoomRepo.save(classRoom);

        // 4. Enroll student
        ClassEnrollment enrollment = new ClassEnrollment();
        enrollment.setClassCode("CSE-HTTP-101");
        enrollment.setClassRoom(classRoom);
        enrollment.setStudent(student);
        classEnrollmentRepo.save(enrollment);

        // 5. Generate valid JWT tokens with JTI
        ownerToken = jwtUtil.generateToken("PROF_HTTP_OWNER", "PROFESSOR", "CSE");
        attackerToken = jwtUtil.generateToken("PROF_HTTP_ATTACKER", "PROFESSOR", "ECE");
    }

    @Test
    @DisplayName("HTTP POST /api/attendance/mark: Cross-professor attendance mark is rejected with 403 Forbidden")
    void httpMarkAttendance_CrossProfessor_Forbidden_NoDatabaseWrite() throws Exception {
        long initialCount = attendanceRepo.count();

        String json = String.format(
                "{\"classCode\":\"CSE-HTTP-101\",\"studentRegNo\":\"STU_HTTP_01\",\"present\":true,\"attendanceDate\":\"%s\"}",
                LocalDate.now()
        );

        mockMvc.perform(post("/api/attendance/mark")
                        .header("Authorization", "Bearer " + attackerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());

        assertEquals(initialCount, attendanceRepo.count(), "No attendance record should be written by unauthorized professor");
    }

    @Test
    @DisplayName("HTTP POST /api/attendance/mark: Authorized owner marks attendance successfully (200 OK)")
    void httpMarkAttendance_AuthorizedOwner_Success() throws Exception {
        String json = String.format(
                "{\"classCode\":\"CSE-HTTP-101\",\"studentRegNo\":\"STU_HTTP_01\",\"present\":true,\"attendanceDate\":\"%s\"}",
                LocalDate.now()
        );

        mockMvc.perform(post("/api/attendance/mark")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        assertEquals(1, attendanceRepo.count());
        assertTrue(attendanceRepo.findByClassCodeAndStudentRegNoAndAttendanceDate(
                "CSE-HTTP-101", "STU_HTTP_01", LocalDate.now()
        ).isPresent());
    }

    @Test
    @DisplayName("HTTP GET /api/professor/classes/{classCode}/students: Cross-professor roster access is rejected with 403 Forbidden")
    void httpGetClassRoster_CrossProfessor_Forbidden() throws Exception {
        mockMvc.perform(get("/api/professor/classes/CSE-HTTP-101/students")
                        .header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HTTP GET /api/professor/classes/{classCode}/students: Authorized owner retrieves class roster successfully (200 OK)")
    void httpGetClassRoster_AuthorizedOwner_Success() throws Exception {
        mockMvc.perform(get("/api/professor/classes/CSE-HTTP-101/students")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("HTTP POST /api/notices/create: Cross-professor notice creation is rejected with 403 Forbidden")
    void httpCreateNotice_CrossProfessor_Forbidden_NoDatabaseWrite() throws Exception {
        long initialNoticeCount = noticeRepo.count();

        CreateNoticeRequest req = new CreateNoticeRequest();
        req.setTitle("Midterm Rescheduled");
        req.setMessage("Please review new schedule");
        req.setClassCodes(List.of("CSE-HTTP-101"));

        mockMvc.perform(post("/api/notices/create")
                        .header("Authorization", "Bearer " + attackerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        assertEquals(initialNoticeCount, noticeRepo.count(), "No notice should be saved by unauthorized professor");
    }

    @Test
    @DisplayName("HTTP POST /api/notices/create: Authorized owner creates notice successfully (200 OK)")
    void httpCreateNotice_AuthorizedOwner_Success() throws Exception {
        CreateNoticeRequest req = new CreateNoticeRequest();
        req.setTitle("Midterm Syllabus");
        req.setMessage("Chapters 1 through 5");
        req.setClassCodes(List.of("CSE-HTTP-101"));

        mockMvc.perform(post("/api/notices/create")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        assertEquals(1, noticeRepo.count());
    }
}
