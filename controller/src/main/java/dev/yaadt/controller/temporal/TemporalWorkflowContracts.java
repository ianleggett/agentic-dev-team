package dev.yaadt.controller.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public final class TemporalWorkflowContracts {
    private TemporalWorkflowContracts() { }
    @WorkflowInterface
    public interface ProjectManagerWorkflow {
        @WorkflowMethod String run(String projectId);
        @io.temporal.workflow.SignalMethod void approve();
    }
    @WorkflowInterface
    public interface SprintWorkflow {
        @WorkflowMethod String run(String sprintId);
        @io.temporal.workflow.SignalMethod void command(String commandId);
    }
    @ActivityInterface
    public interface ControllerActivities {
        @ActivityMethod String acquireLease(String leaseKey, String ownerId);
        @ActivityMethod String heartbeatLease(String leaseKey, String ownerId);
        @ActivityMethod String releaseLease(String leaseKey, String ownerId);
        @ActivityMethod String recordEvent(String ownerId, String eventType);
        @ActivityMethod String executeAgentRun(String runId);
        @ActivityMethod String executeSprint(String sprintId);
        @ActivityMethod String captureCheckpoint(String runId);
        @ActivityMethod String generateProjectPlan(String projectId);
        @ActivityMethod String approveAndExecuteSprint(String sprintId);
        @ActivityMethod String applySprintCommand(String commandId);
    }
}
