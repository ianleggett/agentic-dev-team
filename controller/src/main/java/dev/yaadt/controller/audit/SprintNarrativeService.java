package dev.yaadt.controller.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SprintNarrativeService {
    private final AuditEventRepository audit;
    private final Clock clock;

    public SprintNarrativeService(AuditEventRepository audit, Clock clock) {
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID sprintId, String type, String state, String payload) {
        audit.save(new AuditEvent("SYSTEM", "agent-runner", sprintId, type, null, state, payload, Instant.now(clock)));
    }
}
