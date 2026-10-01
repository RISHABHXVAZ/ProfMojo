package com.profmojo.repository;

import com.profmojo.integration.BasePostgresContainerTest;
import com.profmojo.models.AmenityRequest;
import com.profmojo.models.enums.RequestStatus;
import com.profmojo.repositories.AmenityRequestRepository;
import com.profmojo.testutil.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AmenityRequestRepository PostgreSQL Integration Tests")
class AmenityRequestRepositoryTest extends BasePostgresContainerTest {

    @Autowired
    private AmenityRequestRepository repository;

    @Test
    @DisplayName("Persist AmenityRequest with items and retrieve by department and status")
    void persistAndFindByDepartmentAndStatus() {
        AmenityRequest req = TestDataFactory.createAmenityRequest(null, "PROF01", "CSE", RequestStatus.PENDING);
        req.setItems(List.of("Projector Remote", "Whiteboard Marker"));

        AmenityRequest saved = repository.save(req);
        assertNotNull(saved.getId());

        List<AmenityRequest> results = repository.findByDepartmentAndStatus("CSE", RequestStatus.PENDING);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getItems().size());
        assertTrue(results.get(0).getItems().contains("Projector Remote"));
    }

    @Test
    @DisplayName("findFirstByStatusAndDepartmentOrderByCreatedAtAsc returns oldest request (FIFO)")
    void findFirstByStatusAndDepartment_FifoOrder() {
        LocalDateTime baseTime = LocalDateTime.now().minusMinutes(30);

        AmenityRequest older = TestDataFactory.createAmenityRequest(null, "PROF01", "CSE", RequestStatus.QUEUED);
        older.setCreatedAt(baseTime);

        AmenityRequest newer = TestDataFactory.createAmenityRequest(null, "PROF02", "CSE", RequestStatus.QUEUED);
        newer.setCreatedAt(baseTime.plusMinutes(10));

        repository.save(newer);
        repository.save(older);

        Optional<AmenityRequest> oldest = repository.findFirstByStatusAndDepartmentOrderByCreatedAtAsc(
                RequestStatus.QUEUED, "CSE"
        );

        assertTrue(oldest.isPresent());
        assertEquals("PROF01", oldest.get().getProfessorId(), "Oldest request should be retrieved first in FIFO order");
    }

    @Test
    @DisplayName("markBreachedDeliverySlaRequests: Atomically updates breached requests in real PostgreSQL")
    void markBreachedDeliverySlaRequests_UpdatesFlag() {
        LocalDateTime pastDeadline = LocalDateTime.now().minusMinutes(5);

        AmenityRequest breachedReq = TestDataFactory.createAmenityRequest(null, "PROF01", "CSE", RequestStatus.ASSIGNED);
        breachedReq.setSlaDeadline(pastDeadline);
        breachedReq.setSlaBreached(false);

        AmenityRequest compliantReq = TestDataFactory.createAmenityRequest(null, "PROF02", "CSE", RequestStatus.ASSIGNED);
        compliantReq.setSlaDeadline(LocalDateTime.now().plusMinutes(10));
        compliantReq.setSlaBreached(false);

        repository.save(breachedReq);
        repository.save(compliantReq);

        int updatedCount = repository.markBreachedDeliverySlaRequests(RequestStatus.ASSIGNED, LocalDateTime.now());
        assertEquals(1, updatedCount);

        AmenityRequest reloadedBreached = repository.findById(breachedReq.getId()).orElseThrow();
        assertTrue(reloadedBreached.isSlaBreached());
        assertTrue(reloadedBreached.getDeliverySlaBreached());

        AmenityRequest reloadedCompliant = repository.findById(compliantReq.getId()).orElseThrow();
        assertFalse(reloadedCompliant.isSlaBreached());
    }
}
