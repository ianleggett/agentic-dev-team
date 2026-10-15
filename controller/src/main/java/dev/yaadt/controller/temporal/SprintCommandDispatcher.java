package dev.yaadt.controller.temporal;

import dev.yaadt.controller.sprint.SprintCommand;
import dev.yaadt.controller.sprint.SprintCommandOutbox;
import dev.yaadt.controller.sprint.SprintCommandOutboxRepository;
import dev.yaadt.controller.sprint.SprintCommandRepository;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import java.time.Clock;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SprintCommandDispatcher {
    private final SprintCommandOutboxRepository outbox;
    private final SprintCommandRepository commands;
    private final WorkflowClient client;
    private final TemporalProperties properties;
    private final Clock clock;
    public SprintCommandDispatcher(SprintCommandOutboxRepository outbox, SprintCommandRepository commands,
            WorkflowClient client, TemporalProperties properties, Clock clock) {
        this.outbox = outbox; this.commands = commands; this.client = client; this.properties = properties; this.clock = clock;
    }
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void dispatch() {
        Instant now = Instant.now(clock);
        for (SprintCommandOutbox entry : outbox.findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc("PENDING", now)) {
            SprintCommand command = commands.findById(entry.getCommandId()).orElse(null);
            if (command == null) { entry.delivered(); continue; }
            try {
                String workflowId = "yaadt-sprint-" + command.getSprintId();
                var typed = client.newWorkflowStub(TemporalWorkflowContracts.SprintWorkflow.class,
                        WorkflowOptions.newBuilder().setWorkflowId(workflowId).setTaskQueue(properties.taskQueue()).build());
                WorkflowStub.fromTyped(typed).signalWithStart("command",
                        new Object[] { command.getId().toString() }, new Object[] { command.getSprintId().toString() });
                entry.delivered();
            } catch (RuntimeException failure) {
                entry.retry(failure.getMessage(), now.plusSeconds(Math.min(300, 1L << Math.min(entry.getAttempts(), 8))));
            }
            outbox.save(entry);
        }
    }
}
