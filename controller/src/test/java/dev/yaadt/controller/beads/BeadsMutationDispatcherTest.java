package dev.yaadt.controller.beads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.yaadt.controller.audit.SprintNarrativeService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BeadsMutationDispatcherTest {
    @Mock BeadsMutationOutboxRepository outbox;
    @Mock BeadsClient beads;
    @Mock SprintNarrativeService narrative;

    @Test
    void deliversPendingCloseMutation() {
        Instant now = Instant.parse("2026-10-30T00:00:00Z");
        BeadsMutationOutbox mutation = new BeadsMutationOutbox(UUID.randomUUID(), "key-1", "bd-1", "CLOSE", "completed", now);
        when(outbox.findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc("PENDING", now))
                .thenReturn(List.of(mutation));
        BeadsMutationDispatcher dispatcher = new BeadsMutationDispatcher(outbox, beads, Clock.fixed(now, ZoneOffset.UTC), narrative);

        dispatcher.dispatch();

        verify(beads).closeIssueIfOpen("bd-1", "completed");
        assertThat(mutation.getStatus()).isEqualTo("DELIVERED");
        verify(outbox).save(mutation);
    }

    @Test
    void retriesFailedMutationWithBackoff() {
        Instant now = Instant.parse("2026-10-30T00:00:00Z");
        BeadsMutationOutbox mutation = new BeadsMutationOutbox(UUID.randomUUID(), "key-2", "bd-2", "CLOSE", "completed", now);
        when(outbox.findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc("PENDING", now))
                .thenReturn(List.of(mutation));
        doThrow(new IllegalStateException("Beads unavailable")).when(beads).closeIssueIfOpen("bd-2", "completed");
        BeadsMutationDispatcher dispatcher = new BeadsMutationDispatcher(outbox, beads, Clock.fixed(now, ZoneOffset.UTC), narrative);

        dispatcher.dispatch();

        assertThat(mutation.getStatus()).isEqualTo("PENDING");
        assertThat(mutation.getAttempts()).isEqualTo(1);
        assertThat(mutation.getNextAttemptAt()).isEqualTo(now.plusSeconds(1));
        verify(outbox).save(mutation);
    }
}
