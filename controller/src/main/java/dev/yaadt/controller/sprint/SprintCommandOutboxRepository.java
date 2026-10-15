package dev.yaadt.controller.sprint;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SprintCommandOutboxRepository extends JpaRepository<SprintCommandOutbox, UUID> {
    List<SprintCommandOutbox> findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(String status, Instant now);
}
