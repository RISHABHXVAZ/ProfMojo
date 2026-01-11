package scheduler;

import com.profmojo.models.AmenityRequest;
import com.profmojo.repositories.AmenityRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AmenitySlaMonitor {

    private final AmenityRequestRepository repo;

    @Scheduled(fixedRate = 30000) // every 30 seconds
    public void monitorSla() {

        List<AmenityRequest> active =
                repo.findAll().stream()
                        .filter(r ->
                                "ASSIGNED".equals(r.getStatus()) &&
                                        r.getSlaDeadline() != null &&
                                        LocalDateTime.now().isAfter(r.getSlaDeadline()) &&
                                        !r.isSlaBreached()
                        ).toList();

        for (AmenityRequest r : active) {
            r.setSlaBreached(true);
            repo.save(r);
        }
    }
}

