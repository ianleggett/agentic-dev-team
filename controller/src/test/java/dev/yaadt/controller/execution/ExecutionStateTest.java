package dev.yaadt.controller.execution;

import static org.assertj.core.api.Assertions.assertThat;

import dev.yaadt.controller.executor.AgentRun;
import dev.yaadt.controller.sprint.Sprint;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExecutionStateTest {
    private static final Instant NOW = Instant.parse("2026-10-30T00:00:00Z");

    @Test
    void sprintTransitionsAdvanceVersionAndRecordRecovery() {
        Sprint sprint = new Sprint(UUID.randomUUID(), "objective", UUID.randomUUID(), NOW);

        sprint.transition("RUNNING", NOW.plusSeconds(1));
        sprint.markRecoveryRequired("worker lost", NOW.plusSeconds(2));

        assertThat(sprint.getState()).isEqualTo("RECOVERY_REQUIRED");
        assertThat(sprint.getStateVersion()).isEqualTo(2);
        assertThat(sprint.getRecoveryReason()).isEqualTo("worker lost");
    }

    @Test
    void agentRunBindsTemporalIdentityAndRecordsUnknownOutcome() {
        AgentRun run = new AgentRun(UUID.randomUUID(), 1, "step:key", "/workspace/run", NOW);

        run.bindTemporalExecution("workflow-1", "run-1");
        run.markUnknown("activity result was ambiguous", NOW.plusSeconds(1));

        assertThat(run.getTemporalWorkflowId()).isEqualTo("workflow-1");
        assertThat(run.getTemporalRunId()).isEqualTo("run-1");
        assertThat(run.getState()).isEqualTo("UNKNOWN");
        assertThat(run.getErrorCode()).isEqualTo("UNKNOWN_OUTCOME");
        assertThat(run.getStateVersion()).isEqualTo(1);
    }
}
