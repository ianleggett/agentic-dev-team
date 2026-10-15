package dev.yaadt.controller.temporal;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import dev.yaadt.controller.executor.AgentRunService;

public final class TemporalWorkflowImplementations {
    private TemporalWorkflowImplementations() { }
    private static TemporalWorkflowContracts.ControllerActivities activities() {
        return Workflow.newActivityStub(TemporalWorkflowContracts.ControllerActivities.class,
                ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofMinutes(40))
                        .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                        .build());
    }
    public static class ProjectManager implements TemporalWorkflowContracts.ProjectManagerWorkflow {
        private final TemporalWorkflowContracts.ControllerActivities activities = TemporalWorkflowImplementations.activities();
        private boolean approved;
        @Override public String run(String projectId) {
            String owner = "project-manager:" + projectId;
            activities.acquireLease("project-manager:" + projectId, owner);
            try {
                activities.acquireLease("planning:global", owner);
                String sprintId;
                try {
                    sprintId = activities.generateProjectPlan(projectId);
                } finally {
                    activities.releaseLease("planning:global", owner);
                }
                while (!approved) {
                    activities.heartbeatLease("project-manager:" + projectId, owner);
                    Workflow.await(Duration.ofMinutes(5), () -> approved);
                }
                return activities.approveAndExecuteSprint(sprintId);
            } finally {
                activities.releaseLease("project-manager:" + projectId, owner);
            }
        }
        @Override public void approve() { approved = true; }
    }
    public static class Sprint implements TemporalWorkflowContracts.SprintWorkflow {
        private final TemporalWorkflowContracts.ControllerActivities activities = TemporalWorkflowImplementations.activities();
        private final List<String> pendingCommands = new ArrayList<>();
        @Override public String run(String sprintId) {
            while (true) {
                Workflow.await(() -> !pendingCommands.isEmpty());
                String commandId = pendingCommands.remove(0);
                activities.applySprintCommand(commandId);
            }
        }
        @Override public void command(String commandId) { if (!pendingCommands.contains(commandId)) pendingCommands.add(commandId); }
    }
    public static class Activities implements TemporalWorkflowContracts.ControllerActivities {
        private final AgentRunService runs;
        private final dev.yaadt.controller.manager.ProjectManagerPlanningService planning;
        private final dev.yaadt.controller.sprint.SprintCommandService commands;
        private final dev.yaadt.controller.sprint.SprintPlanVersionRepository plans;
        private final dev.yaadt.controller.manager.ControllerLeaseService leases;
        public Activities(AgentRunService runs, dev.yaadt.controller.manager.ProjectManagerPlanningService planning,
                dev.yaadt.controller.sprint.SprintCommandService commands, dev.yaadt.controller.sprint.SprintPlanVersionRepository plans,
                dev.yaadt.controller.manager.ControllerLeaseService leases) {
            this.runs = runs; this.planning = planning; this.commands = commands; this.plans = plans;
            this.leases = leases;
        }
        @Override public String acquireLease(String leaseKey, String ownerId) { return leases.acquire(leaseKey, ownerId); }
        @Override public String heartbeatLease(String leaseKey, String ownerId) { return leases.heartbeat(leaseKey, ownerId); }
        @Override public String releaseLease(String leaseKey, String ownerId) { leases.release(leaseKey, ownerId); return ownerId; }
        @Override public String recordEvent(String ownerId, String eventType) { return eventType; }
        @Override public String executeAgentRun(String runId) { Activity.getExecutionContext().heartbeat("running"); return runs.executeRun(UUID.fromString(runId)).id().toString(); }
        @Override public String executeSprint(String sprintId) { Activity.getExecutionContext().heartbeat("starting sprint"); return runs.executeSprint(UUID.fromString(sprintId)); }
        @Override public String captureCheckpoint(String runId) { return runId; }
        @Override public String generateProjectPlan(String projectId) { return planning.generate(UUID.fromString(projectId)).toString(); }
        @Override public String approveAndExecuteSprint(String sprintId) {
            UUID id = UUID.fromString(sprintId);
            int version = plans.findTopBySprintIdOrderByVersionDesc(id).orElseThrow().getVersion();
            commands.command(id, "approve", new dev.yaadt.controller.sprint.SprintCommandRequest(version, "project manager approval"), "project-manager", "Project Manager", null);
            return runs.executeSprint(id);
        }
        @Override public String applySprintCommand(String commandId) { return commands.apply(UUID.fromString(commandId)).status(); }
    }
}
