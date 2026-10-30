package dev.yaadt.controller.beads;

import dev.yaadt.controller.audit.SprintNarrativeService;
import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BeadsMutationDispatcher {
    private final BeadsMutationOutboxRepository outbox;
    private final BeadsClient beads;
    private final Clock clock;
    private final SprintNarrativeService narrative;

    public BeadsMutationDispatcher(BeadsMutationOutboxRepository outbox, BeadsClient beads, Clock clock,
            SprintNarrativeService narrative) {
        this.outbox = outbox; this.beads = beads; this.clock = clock; this.narrative = narrative;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void dispatch() {
        Instant now = Instant.now(clock);
        for (BeadsMutationOutbox mutation : outbox.findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc("PENDING", now)) {
            try {
                if ("CLOSE".equals(mutation.getOperation())) {
                    beads.closeIssueIfOpen(mutation.getBeadId(), mutation.getReason());
                } else {
                    throw new IllegalStateException("Unsupported Beads mutation: " + mutation.getOperation());
                }
                mutation.delivered();
                narrative.record(mutation.getSprintId(), "bead-completed", "COMPLETED", "bead=" + mutation.getBeadId());
            } catch (RuntimeException failure) {
                mutation.retry(failure.getMessage(), now.plusSeconds(Math.min(300, 1L << Math.min(mutation.getAttempts(), 8))));
            }
            outbox.save(mutation);
        }
    }
}
