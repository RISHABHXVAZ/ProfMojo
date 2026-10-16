package com.profmojo.security;

import com.profmojo.models.ClassRoom;
import com.profmojo.models.Notice;
import com.profmojo.models.NoticeClassMapping;
import com.profmojo.models.Professor;
import com.profmojo.models.dto.CreateNoticeRequest;
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
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 1 Security - Notice Creation Class Ownership Tests")
class NoticeSecurityTest {

    @Mock
    private NoticeRepository noticeRepo;

    @Mock
    private NoticeClassMappingRepository mappingRepo;

    @Mock
    private ClassRoomRepository classRoomRepo;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    private Professor ownerProf;
    private Professor foreignProf;
    private ClassRoom ownedClass1;
    private ClassRoom ownedClass2;
    private ClassRoom foreignClass;

    @BeforeEach
    void setUp() {
        ownerProf = new Professor();
        ownerProf.setProfId("PROF_OWNER");
        ownerProf.setName("Dr. Owner");

        foreignProf = new Professor();
        foreignProf.setProfId("PROF_FOREIGN");
        foreignProf.setName("Dr. Foreign");

        ownedClass1 = new ClassRoom();
        ownedClass1.setClassCode("CSE-A");
        ownedClass1.setProfessor(ownerProf);

        ownedClass2 = new ClassRoom();
        ownedClass2.setClassCode("CSE-B");
        ownedClass2.setProfessor(ownerProf);

        foreignClass = new ClassRoom();
        foreignClass.setClassCode("ECE-X");
        foreignClass.setProfessor(foreignProf);
    }

    @Test
    @DisplayName("createNotice: Unauthenticated professor throws AccessDeniedException")
    void createNotice_NullProfessor_ThrowsAccessDenied() {
        CreateNoticeRequest req = new CreateNoticeRequest();
        req.setTitle("Important Notice");
        req.setMessage("Please review");
        req.setClassCodes(List.of("CSE-A"));

        assertThrows(AccessDeniedException.class, () ->
                noticeService.createNotice(req, null)
        );

        verify(noticeRepo, never()).save(any());
        verify(mappingRepo, never()).save(any());
    }

    @Test
    @DisplayName("createNotice: Empty classCodes throws IllegalArgumentException")
    void createNotice_EmptyClassCodes_ThrowsIllegalArgument() {
        CreateNoticeRequest req = new CreateNoticeRequest();
        req.setTitle("Important Notice");
        req.setMessage("Please review");
        req.setClassCodes(Collections.emptyList());

        assertThrows(IllegalArgumentException.class, () ->
                noticeService.createNotice(req, ownerProf)
        );

        verify(noticeRepo, never()).save(any());
        verify(mappingRepo, never()).save(any());
    }

    @Test
    @DisplayName("createNotice: Targeting a foreign class code throws AccessDeniedException and aborts notice creation")
    void createNotice_TargetingForeignClass_ThrowsAccessDeniedAndAborts() {
        CreateNoticeRequest req = new CreateNoticeRequest();
        req.setTitle("Exam Schedule");
        req.setMessage("Test tomorrow");
        req.setClassCodes(List.of("CSE-A", "ECE-X"));

        when(classRoomRepo.findByClassCode("CSE-A")).thenReturn(Optional.of(ownedClass1));
        when(classRoomRepo.findByClassCode("ECE-X")).thenReturn(Optional.of(foreignClass));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                noticeService.createNotice(req, ownerProf)
        );
        assertTrue(ex.getMessage().contains("do not own class ECE-X"));

        // Crucial transactional verification: neither notice nor mapping should be persisted!
        verify(noticeRepo, never()).save(any());
        verify(mappingRepo, never()).save(any());
    }

    @Test
    @DisplayName("createNotice: Non-existent class code throws IllegalArgumentException and aborts")
    void createNotice_NonExistentClass_ThrowsIllegalArgumentAndAborts() {
        CreateNoticeRequest req = new CreateNoticeRequest();
        req.setTitle("Exam Schedule");
        req.setMessage("Test tomorrow");
        req.setClassCodes(List.of("UNKNOWN_CLASS"));

        when(classRoomRepo.findByClassCode("UNKNOWN_CLASS")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                noticeService.createNotice(req, ownerProf)
        );

        verify(noticeRepo, never()).save(any());
        verify(mappingRepo, never()).save(any());
    }

    @Test
    @DisplayName("createNotice: Authorized professor targeting owned classes succeeds and saves all mappings")
    void createNotice_AllOwnedClasses_Success() {
        CreateNoticeRequest req = new CreateNoticeRequest();
        req.setTitle("Welcome");
        req.setMessage("Welcome to semester");
        req.setClassCodes(List.of("CSE-A", "CSE-B"));

        when(classRoomRepo.findByClassCode("CSE-A")).thenReturn(Optional.of(ownedClass1));
        when(classRoomRepo.findByClassCode("CSE-B")).thenReturn(Optional.of(ownedClass2));

        noticeService.createNotice(req, ownerProf);

        verify(noticeRepo, times(1)).save(any(Notice.class));
        verify(mappingRepo, times(2)).save(any(NoticeClassMapping.class));
    }
}
