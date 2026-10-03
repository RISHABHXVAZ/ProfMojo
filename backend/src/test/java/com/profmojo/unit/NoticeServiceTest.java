package com.profmojo.unit;

import com.profmojo.models.Notice;
import com.profmojo.models.NoticeClassMapping;
import com.profmojo.models.Professor;
import com.profmojo.models.dto.NoticeResponseDTO;
import com.profmojo.repositories.ClassRoomRepository;
import com.profmojo.repositories.NoticeClassMappingRepository;
import com.profmojo.repositories.NoticeRepository;
import com.profmojo.services.impl.NoticeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeService N+1 Batch Optimization Unit Tests")
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepo;

    @Mock
    private NoticeClassMappingRepository mappingRepo;

    @Mock
    private ClassRoomRepository classRoomRepo;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    private Professor professor;

    @BeforeEach
    void setUp() {
        professor = new Professor();
        professor.setProfId("PROF001");
        professor.setName("Dr. Alan Turing");
        professor.setEmail("alan.turing@university.edu");
    }

    @Test
    @DisplayName("Zero notices: returns empty list without issuing any mapping queries")
    void getProfessorNotices_whenZeroNotices_returnsEmptyWithoutMappingQueries() {
        when(noticeRepo.findByCreatedByOrderByCreatedAtDesc(professor))
                .thenReturn(Collections.emptyList());

        List<NoticeResponseDTO> result = noticeService.getProfessorNotices(professor);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(noticeRepo, times(1)).findByCreatedByOrderByCreatedAtDesc(professor);
        verify(mappingRepo, never()).findByNoticeIn(any());
        verify(mappingRepo, never()).findByNotice(any());
    }

    @Test
    @DisplayName("One notice with mappings: returns correct DTO response structure")
    void getProfessorNotices_whenOneNotice_returnsCorrectMappingsAndResponseStructure() {
        LocalDateTime now = LocalDateTime.now();
        Notice notice = new Notice(1L, "Exam Schedule", "Final exams begin next week.", now, professor);

        NoticeClassMapping map1 = new NoticeClassMapping(101L, notice, "CSE101");
        NoticeClassMapping map2 = new NoticeClassMapping(102L, notice, "CSE102");

        when(noticeRepo.findByCreatedByOrderByCreatedAtDesc(professor))
                .thenReturn(List.of(notice));
        when(mappingRepo.findByNoticeIn(List.of(notice)))
                .thenReturn(List.of(map1, map2));

        List<NoticeResponseDTO> result = noticeService.getProfessorNotices(professor);

        assertEquals(2, result.size());

        NoticeResponseDTO dto1 = result.get(0);
        assertEquals("Exam Schedule", dto1.getTitle());
        assertEquals("Final exams begin next week.", dto1.getMessage());
        assertEquals("CSE101", dto1.getClassCode());
        assertEquals(now, dto1.getCreatedAt());
        assertEquals("Dr. Alan Turing", dto1.getProfessorName());

        NoticeResponseDTO dto2 = result.get(1);
        assertEquals("Exam Schedule", dto2.getTitle());
        assertEquals("Final exams begin next week.", dto2.getMessage());
        assertEquals("CSE102", dto2.getClassCode());
        assertEquals(now, dto2.getCreatedAt());
        assertEquals("Dr. Alan Turing", dto2.getProfessorName());

        verify(mappingRepo, times(1)).findByNoticeIn(any());
        verify(mappingRepo, never()).findByNotice(any());
    }

    @Test
    @DisplayName("Multiple notices: preserves notice ordering and correctly associates mappings per notice")
    void getProfessorNotices_whenMultipleNotices_preservesOrderAndMapsCorrectly() {
        LocalDateTime t3 = LocalDateTime.now();
        LocalDateTime t2 = t3.minusHours(1);
        LocalDateTime t1 = t3.minusHours(2);

        Notice n1 = new Notice(1L, "Notice Newest", "Msg 1", t3, professor);
        Notice n2 = new Notice(2L, "Notice Middle", "Msg 2", t2, professor);
        Notice n3 = new Notice(3L, "Notice Oldest", "Msg 3", t1, professor);

        List<Notice> notices = List.of(n1, n2, n3);

        NoticeClassMapping m1_1 = new NoticeClassMapping(11L, n1, "CSE101");
        NoticeClassMapping m1_2 = new NoticeClassMapping(12L, n1, "CSE102");
        NoticeClassMapping m2_1 = new NoticeClassMapping(21L, n2, "ECE201");
        NoticeClassMapping m3_1 = new NoticeClassMapping(31L, n3, "MECH301");

        when(noticeRepo.findByCreatedByOrderByCreatedAtDesc(professor))
                .thenReturn(notices);
        when(mappingRepo.findByNoticeIn(notices))
                .thenReturn(List.of(m1_1, m1_2, m2_1, m3_1));

        List<NoticeResponseDTO> result = noticeService.getProfessorNotices(professor);

        assertEquals(4, result.size());

        // Verify order is preserved: n1 mappings first, then n2, then n3
        assertEquals("Notice Newest", result.get(0).getTitle());
        assertEquals("CSE101", result.get(0).getClassCode());
        assertEquals(t3, result.get(0).getCreatedAt());

        assertEquals("Notice Newest", result.get(1).getTitle());
        assertEquals("CSE102", result.get(1).getClassCode());
        assertEquals(t3, result.get(1).getCreatedAt());

        assertEquals("Notice Middle", result.get(2).getTitle());
        assertEquals("ECE201", result.get(2).getClassCode());
        assertEquals(t2, result.get(2).getCreatedAt());

        assertEquals("Notice Oldest", result.get(3).getTitle());
        assertEquals("MECH301", result.get(3).getClassCode());
        assertEquals(t1, result.get(3).getCreatedAt());

        verify(mappingRepo, times(1)).findByNoticeIn(notices);
        verify(mappingRepo, never()).findByNotice(any());
    }

    @Test
    @DisplayName("Notice with no mappings: behaves gracefully without error")
    void getProfessorNotices_whenNoticeHasNoMappings_handlesGracefully() {
        LocalDateTime now = LocalDateTime.now();
        Notice n1WithMappings = new Notice(1L, "With Mappings", "Msg 1", now, professor);
        Notice n2NoMappings = new Notice(2L, "Orphaned Notice", "Msg 2", now.minusMinutes(5), professor);

        List<Notice> notices = List.of(n1WithMappings, n2NoMappings);
        NoticeClassMapping m1 = new NoticeClassMapping(10L, n1WithMappings, "CSE101");

        when(noticeRepo.findByCreatedByOrderByCreatedAtDesc(professor))
                .thenReturn(notices);
        when(mappingRepo.findByNoticeIn(notices))
                .thenReturn(List.of(m1));

        List<NoticeResponseDTO> result = noticeService.getProfessorNotices(professor);

        assertEquals(1, result.size());
        assertEquals("With Mappings", result.get(0).getTitle());
        assertEquals("CSE101", result.get(0).getClassCode());

        verify(mappingRepo, times(1)).findByNoticeIn(notices);
        verify(mappingRepo, never()).findByNotice(any());
    }

    @Test
    @DisplayName("Regression Test: N notices must trigger exactly ONE batch mapping query, never N queries")
    void getProfessorNotices_regressionTest_verifiesBatchInvocationAndPreventsNPlusOne() {
        int n = 20;
        List<Notice> notices = new ArrayList<>();
        List<NoticeClassMapping> allMappings = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now();

        for (long i = 1; i <= n; i++) {
            Notice notice = new Notice(i, "Notice " + i, "Body " + i, baseTime.minusMinutes(i), professor);
            notices.add(notice);
            allMappings.add(new NoticeClassMapping(i * 10, notice, "CLASS_" + i));
            allMappings.add(new NoticeClassMapping(i * 10 + 1, notice, "CLASS_" + (i + 100)));
        }

        when(noticeRepo.findByCreatedByOrderByCreatedAtDesc(professor)).thenReturn(notices);
        when(mappingRepo.findByNoticeIn(notices)).thenReturn(allMappings);

        List<NoticeResponseDTO> result = noticeService.getProfessorNotices(professor);

        assertEquals(40, result.size());

        // Batch query must be invoked exactly once
        verify(mappingRepo, times(1)).findByNoticeIn(notices);

        // Crucial regression check: Old N+1 method findByNotice(n) must NEVER be called
        verify(mappingRepo, never()).findByNotice(any(Notice.class));
    }
}
