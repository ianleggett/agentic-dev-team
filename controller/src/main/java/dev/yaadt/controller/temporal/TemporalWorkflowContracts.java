package dev.yaadt.controller.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

public final class TemporalWorkflowContracts {
    private TemporalWorkflowContracts() { }
    @WorkflowInterface
    public interface ProjectManagerWorkflow { @WorkflowMethod String run(String projectId); }
    @WorkflowInterface
    public interface SprintWorkflow { @WorkflowMethod String run(String sprintId); }
    @ActivityInterface
    public interface ControllerActivities {
        @ActivityMethod String acquireLease(String ownerId);
        @ActivityMethod String recordEvent(String ownerId, String eventType);
        @ActivityMethod String executeAgentRun(String runId);
        @ActivityMethod String captureCheckpoint(String runId);
    }
}
