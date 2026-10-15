package dev.yaadt.controller.executor;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {
    Optional<AgentRun> findByIdempotencyKey(String idempotencyKey);
    List<AgentRun> findByStepIdOrderByAttemptNumberDesc(UUID stepId);
}
