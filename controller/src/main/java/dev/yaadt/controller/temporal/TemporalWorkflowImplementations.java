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
import dev.yaadt.controller.beads.BeadsClient;
import dev.yaadt.controller.beads.BeadsProjectLabels;
import dev.yaadt.controller.beads.BeadsStatus;
import dev.yaadt.controller.project.ProjectRepository;
import dev.yaadt.controller.manager.ProjectManagerRepository;

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
                int autonomousVersion = Workflow.getVersion("autonomous-supervisor-loop", Workflow.DEFAULT_VERSION, 1);
                if (autonomousVersion == 1 && activities.isAutonomous(projectId)) {
                    while (true) {
                        String result = activities.approveAndExecuteSprint(sprintId);
                        if (result.startsWith("BLOCKED:")) {
                            activities.setProjectManagerStatus(projectId, "BLOCKED", result);
                            return result;
                        }
                        if (!activities.hasRemainingProjectWork(projectId)) {
                            activities.setProjectManagerStatus(projectId, "COMPLETED", "No remaining non-terminal project Beads");
                            return "COMPLETED: no remaining project Beads";
                        }
                        activities.acquireLease("planning:global", owner);
                        try {
                            sprintId = activities.generateProjectPlan(projectId);
                        } finally {
                            activities.releaseLease("planning:global", owner);
                        }
                    }
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
        private final dev.yaadt.controller.sprint.SprintRepository sprints;
        private final dev.yaadt.controller.delivery.DeliveryPlanRepository deliveryPlans;
        private final dev.yaadt.controller.delivery.DeliveryPlanService deliveryPlanService;
        private final dev.yaadt.controller.delivery.DeliveryPlanSchedulerService scheduler;
        private final ProjectRepository projects;
        private final BeadsClient beads;
        private final ProjectManagerRepository managers;
        public Activities(AgentRunService runs, dev.yaadt.controller.manager.ProjectManagerPlanningService planning,
                dev.yaadt.controller.sprint.SprintCommandService commands, dev.yaadt.controller.sprint.SprintPlanVersionRepository plans,
                dev.yaadt.controller.manager.ControllerLeaseService leases, dev.yaadt.controller.sprint.SprintRepository sprints,
                dev.yaadt.controller.delivery.DeliveryPlanRepository deliveryPlans,
                dev.yaadt.controller.delivery.DeliveryPlanService deliveryPlanService,
                dev.yaadt.controller.delivery.DeliveryPlanSchedulerService scheduler,
                ProjectRepository projects, BeadsClient beads, ProjectManagerRepository managers) {
            this.runs = runs; this.planning = planning; this.commands = commands; this.plans = plans;
            this.leases = leases; this.sprints = sprints; this.deliveryPlans = deliveryPlans;
            this.deliveryPlanService = deliveryPlanService; this.scheduler = scheduler;
            this.projects = projects; this.beads = beads;
            this.managers = managers;
        }
        @Override public String acquireLease(String leaseKey, String ownerId) { return leases.acquire(leaseKey, ownerId); }
        @Override public String heartbeatLease(String leaseKey, String ownerId) { return leases.heartbeat(leaseKey, ownerId); }
        @Override public String releaseLease(String leaseKey, String ownerId) { leases.release(leaseKey, ownerId); return ownerId; }
        @Override public String recordEvent(String ownerId, String eventType) { return eventType; }
        @Override public String executeAgentRun(String runId) { Activity.getExecutionContext().heartbeat("running"); return runs.executeRun(UUID.fromString(runId)).id().toString(); }
        @Override public String executeSprint(String sprintId) {
            Activity.getExecutionContext().heartbeat("starting sprint");
            var info = Activity.getExecutionContext().getInfo();
            return runs.executeSprint(UUID.fromString(sprintId), info.getWorkflowId(), info.getRunId());
        }
        @Override public String captureCheckpoint(String runId) { return runId; }
        @Override public String generateProjectPlan(String projectId) {
            UUID id = UUID.fromString(projectId);
            try {
                return planning.generate(id).toString();
            } catch (RuntimeException failure) {
                planning.recordFailure(id, failure.getMessage());
                throw failure;
            }
        }
        @Override public boolean isAutonomous(String projectId) {
            return projects.findById(UUID.fromString(projectId)).map(project -> !project.isApprovalRequired()).orElse(false);
        }
        @Override public boolean hasRemainingProjectWork(String projectId) {
            var project = projects.findById(UUID.fromString(projectId)).orElseThrow(() -> new IllegalArgumentException("Project not found"));
            return beads.listRelevantIssues(BeadsProjectLabels.forProject(project.getName())).stream()
                    .anyMatch(issue -> !BeadsStatus.isTerminal(issue.status()));
        }
        @Override public String setProjectManagerStatus(String projectId, String status, String detail) {
            managers.findByProjectId(UUID.fromString(projectId)).ifPresent(manager -> {
                manager.setState(status, java.time.Instant.now());
                if ("BLOCKED".equals(status)) manager.recordPlanningFailure(detail, java.time.Instant.now());
                managers.save(manager);
            });
            return status;
        }
        @Override public String approveAndExecuteSprint(String sprintId) {
            UUID id = UUID.fromString(sprintId);
            var deliveryPlan = deliveryPlans.findById(id).orElse(null);
            if (deliveryPlan != null) {
                if (!"DRAFT".equals(deliveryPlan.getState())) {
                    throw new IllegalStateException("DeliveryPlan is not awaiting approval: " + deliveryPlan.getState());
                }
                deliveryPlanService.submit(deliveryPlan.getId());
                deliveryPlanService.approve(deliveryPlan.getId(),
                        new dev.yaadt.controller.delivery.DeliveryPlanDecisionRequest(deliveryPlan.getCurrentVersion(), "Project Manager approval"),
                        "project-manager");
                dev.yaadt.controller.sprint.SprintResponse generated;
                try {
                    generated = scheduler.generateNextSprint(deliveryPlan.getId(), "project-manager", "Project Manager");
                } catch (RuntimeException blocked) {
                    return "BLOCKED: " + (blocked.getMessage() == null ? blocked.getClass().getSimpleName() : blocked.getMessage());
                }
                var info = Activity.getExecutionContext().getInfo();
                return runs.executeSprint(generated.id(), info.getWorkflowId(), info.getRunId());
            }
            throw new IllegalStateException("DeliveryPlan not found: " + id);
        }
        @Override public String applySprintCommand(String commandId) { return commands.apply(UUID.fromString(commandId)).status(); }
    }
}
