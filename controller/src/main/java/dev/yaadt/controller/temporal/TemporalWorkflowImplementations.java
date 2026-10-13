package dev.yaadt.controller.temporal;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public final class TemporalWorkflowImplementations {
    private TemporalWorkflowImplementations() { }
    private static TemporalWorkflowContracts.ControllerActivities activities() {
        return Workflow.newActivityStub(TemporalWorkflowContracts.ControllerActivities.class,
                ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofMinutes(10))
                        .setHeartbeatTimeout(Duration.ofSeconds(30))
                        .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                        .build());
    }
    public static class ProjectManager implements TemporalWorkflowContracts.ProjectManagerWorkflow {
        private final TemporalWorkflowContracts.ControllerActivities activities = TemporalWorkflowImplementations.activities();
        @Override public String run(String projectId) { Workflow.await(() -> false); return "STOPPED"; }
    }
    public static class Sprint implements TemporalWorkflowContracts.SprintWorkflow {
        private final TemporalWorkflowContracts.ControllerActivities activities = TemporalWorkflowImplementations.activities();
        @Override public String run(String sprintId) { Workflow.await(() -> false); return "STOPPED"; }
    }
    public static class Activities implements TemporalWorkflowContracts.ControllerActivities {
        @Override public String acquireLease(String ownerId) { return ownerId; }
        @Override public String recordEvent(String ownerId, String eventType) { return eventType; }
        @Override public String executeAgentRun(String runId) { Activity.getExecutionContext().heartbeat("running"); return runId; }
        @Override public String captureCheckpoint(String runId) { return runId; }
    }
}
