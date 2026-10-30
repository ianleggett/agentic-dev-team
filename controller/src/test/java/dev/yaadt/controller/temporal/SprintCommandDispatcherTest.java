package dev.yaadt.controller.temporal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.yaadt.controller.sprint.SprintCommand;
import dev.yaadt.controller.sprint.SprintCommandOutbox;
import dev.yaadt.controller.sprint.SprintCommandOutboxRepository;
import dev.yaadt.controller.sprint.SprintCommandRepository;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SprintCommandDispatcherTest {
    @Mock SprintCommandOutboxRepository outbox;
    @Mock SprintCommandRepository commands;
    @Mock WorkflowClient client;

    @Test
    void temporalDeliveryFailureRetriesTheSameOutboxCommand() {
        Instant now = Instant.parse("2026-10-30T00:00:00Z");
        UUID commandId = UUID.randomUUID();
        UUID sprintId = UUID.randomUUID();
        SprintCommandOutbox entry = new SprintCommandOutbox(commandId, now);
        SprintCommand command = new SprintCommand(sprintId, "request-1", "approve", 1, "approve", "{}", "user", "User", now);
        TemporalProperties properties = new TemporalProperties(false, "temporal:7233", "default", "yaadt");
        when(outbox.findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc("PENDING", now))
                .thenReturn(List.of(entry));
        when(commands.findById(commandId)).thenReturn(Optional.of(command));
        when(client.newWorkflowStub(any(Class.class), any(WorkflowOptions.class)))
                .thenThrow(new IllegalStateException("Temporal unavailable"));

        SprintCommandDispatcher dispatcher = new SprintCommandDispatcher(outbox, commands, client, properties,
                Clock.fixed(now, ZoneOffset.UTC));
        dispatcher.dispatch();

        assertThat(entry.getStatus()).isEqualTo("PENDING");
        assertThat(entry.getAttempts()).isEqualTo(1);
        assertThat(entry.getNextAttemptAt()).isEqualTo(now.plusSeconds(1));
        verify(outbox).save(entry);
    }
}
