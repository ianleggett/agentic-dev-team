package dev.yaadt.controller.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import dev.yaadt.controller.audit.SprintNarrativeService;
import dev.yaadt.controller.beads.BeadsClient;
import dev.yaadt.controller.beads.BeadsMutationOutboxRepository;
import dev.yaadt.controller.manager.ControllerLeaseService;
import dev.yaadt.controller.profile.AgentProfileRepository;
import dev.yaadt.controller.profile.AgentProfileVersionRepository;
import dev.yaadt.controller.project.ProjectRepository;
import dev.yaadt.controller.repository.RepositoryRepository;
import dev.yaadt.controller.sprint.SprintPlanVersionRepository;
import dev.yaadt.controller.sprint.SprintRepository;
import dev.yaadt.controller.sprint.SprintStepRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import dev.yaadt.controller.delivery.DeliveryPlanSchedulerService;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentRunServiceTest {
    @Mock AgentRunRepository runs;
    @Mock RunEventRepository events;
    @Mock AgentExecutor executor;
    @Mock SprintPlanVersionRepository plans;
    @Mock SprintStepRepository steps;
    @Mock AgentProfileVersionRepository profiles;
    @Mock AgentProfileRepository profileDefinitions;
    @Mock SprintRepository sprints;
    @Mock SprintNarrativeService narrative;
    @Mock RepositoryRepository repositories;
    @Mock ControllerLeaseService leases;
    @Mock ProjectRepository projects;
    @Mock BeadsClient beads;
    @Mock BeadsMutationOutboxRepository beadMutations;
    @Mock DeliveryPlanSchedulerService deliveryPlans;

    private AgentRunService service;
    private final UUID stepId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-30T00:00:00Z");

    @BeforeEach
    void setUp() {
        service = new AgentRunService(runs, events, executor, plans, steps, profiles, profileDefinitions,
                sprints, narrative, repositories, Clock.fixed(now, ZoneOffset.UTC), leases, projects, beads, beadMutations, deliveryPlans);
    }

    @Test
    void failedAttemptCreatesTheNextIdempotentAttempt() {
        AgentRun failed = new AgentRun(stepId, 1, "step:" + stepId + ":attempt:1", "/workspace/runs/" + stepId, now);
        failed.finish(new AgentRunResult("attempt-1", AgentRunResult.State.FAILED, null, "FAILED", "tests failed", List.of()), now);
        when(runs.findByStepIdOrderByAttemptNumberDesc(stepId)).thenReturn(List.of(failed));
        when(runs.findByIdempotencyKey("step:" + stepId + ":attempt:2")).thenReturn(Optional.empty());
        when(runs.save(any(AgentRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AgentRunResponse response = service.createRun(stepId);

        assertThat(response.attemptNumber()).isEqualTo(2);
        assertThat(response.state()).isEqualTo("CREATED");
        verify(runs).save(any(AgentRun.class));
    }

    @Test
    void terminalAttemptDoesNotLaunchTheExecutorAgain() {
        UUID runId = UUID.randomUUID();
        AgentRun succeeded = new AgentRun(stepId, 1, "step:" + stepId + ":attempt:1", "/workspace/runs/" + stepId, now);
        succeeded.finish(new AgentRunResult("attempt-1", AgentRunResult.State.SUCCEEDED, "abc123", null, null, List.of()), now);
        when(runs.findById(runId)).thenReturn(Optional.of(succeeded));

        AgentRunResponse response = service.executeRun(runId);

        assertThat(response.state()).isEqualTo("SUCCEEDED");
        verify(executor, never()).execute(any(AgentRunRequest.class));
    }

    @Test
    void unknownAttemptDoesNotCreateAReplacementRun() {
        AgentRun unknown = new AgentRun(stepId, 1, "step:" + stepId + ":attempt:1", "/workspace/runs/" + stepId, now);
        unknown.finish(new AgentRunResult("attempt-1", AgentRunResult.State.UNKNOWN, null,
                "RUNNER_UNAVAILABLE", "outcome cannot be determined", List.of()), now);
        when(runs.findByStepIdOrderByAttemptNumberDesc(stepId)).thenReturn(List.of(unknown));

        AgentRunResponse response = service.createRun(stepId);

        assertThat(response.state()).isEqualTo("UNKNOWN");
        assertThat(response.attemptNumber()).isEqualTo(1);
        verify(runs, never()).save(any(AgentRun.class));
    }

    @Test
    void completedSprintIsNotExecutedAgain() {
        UUID sprintId = UUID.randomUUID();
        dev.yaadt.controller.sprint.SprintPlanVersion plan = mock(dev.yaadt.controller.sprint.SprintPlanVersion.class);
        dev.yaadt.controller.sprint.Sprint completed = mock(dev.yaadt.controller.sprint.Sprint.class);
        when(plans.findTopBySprintIdOrderByVersionDesc(sprintId)).thenReturn(Optional.of(plan));
        when(sprints.findById(sprintId)).thenReturn(Optional.of(completed));
        when(completed.getState()).thenReturn("COMPLETED");

        assertThat(service.executeSprint(sprintId)).isEqualTo("ALREADY_COMPLETED");
        verifyNoInteractions(executor, leases, narrative);
    }
}
