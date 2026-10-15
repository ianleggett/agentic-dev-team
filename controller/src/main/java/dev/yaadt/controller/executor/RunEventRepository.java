package dev.yaadt.controller.executor;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunEventRepository extends JpaRepository<RunEvent, UUID> {
    List<RunEvent> findByAgentRunIdOrderBySequenceNumberAsc(UUID agentRunId);
}
