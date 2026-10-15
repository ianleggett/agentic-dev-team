package dev.yaadt.controller.sprint;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SprintCommandLifecycleTest {
    @Test
    void commandMovesFromReceivedToAppliedWithoutChangingIdentity() {
        UUID sprintId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        SprintCommand command = new SprintCommand(sprintId, "request-1", "approve", 1, "ship", "{}", "user", "User", now);

        UUID commandId = command.getId();
        command.applied("READY", now.plusSeconds(1));

        assertThat(command.getId()).isEqualTo(commandId);
        assertThat(command.getStatus()).isEqualTo("APPLIED");
        assertThat(command.getResultState()).isEqualTo("READY");
    }

    @Test
    void outboxRetryPreservesCommandAndSchedulesNextAttempt() {
        UUID commandId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        SprintCommandOutbox outbox = new SprintCommandOutbox(commandId, now);

        outbox.retry("Temporal unavailable", now.plusSeconds(8));

        assertThat(outbox.getCommandId()).isEqualTo(commandId);
        assertThat(outbox.getAttempts()).isEqualTo(1);
        assertThat(outbox.getNextAttemptAt()).isEqualTo(now.plusSeconds(8));
        assertThat(outbox.getStatus()).isEqualTo("PENDING");
    }
}
