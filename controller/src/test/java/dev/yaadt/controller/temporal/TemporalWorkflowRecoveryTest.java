package dev.yaadt.controller.temporal;

import static org.assertj.core.api.Assertions.assertThat;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TemporalWorkflowRecoveryTest {
    private static final String TASK_QUEUE = "temporal-recovery-test";
    private TestWorkflowEnvironment environment;
    private CountingActivities activities;
    private Worker worker;

    @BeforeEach
    void setUp() {
        environment = TestWorkflowEnvironment.newInstance();
        worker = environment.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(TemporalWorkflowImplementations.Sprint.class);
        activities = new CountingActivities();
        worker.registerActivitiesImplementations(activities);
        environment.start();
    }

    @AfterEach
    void tearDown() {
        environment.close();
    }

    @Test
    void duplicateSignalsApplyTheCommandOnce() throws Exception {
        TemporalWorkflowContracts.SprintWorkflow workflow = environment.getWorkflowClient().newWorkflowStub(
                TemporalWorkflowContracts.SprintWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId("sprint-recovery-1").build());
        WorkflowClient.start(workflow::run, "sprint-1");

        workflow.command("command-1");
        workflow.command("command-1");

        assertThat(activities.firstApplied.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(activities.invocations.get()).isEqualTo(1);
        worker.replayWorkflowExecution(environment.getWorkflowExecutionHistory(WorkflowStub.fromTyped(workflow).getExecution()));
    }

    @Test
    void activityFailureIsRetriedWithoutASecondSignal() throws Exception {
        activities.failFirst = true;
        TemporalWorkflowContracts.SprintWorkflow workflow = environment.getWorkflowClient().newWorkflowStub(
                TemporalWorkflowContracts.SprintWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId("sprint-recovery-2").build());
        WorkflowClient.start(workflow::run, "sprint-2");

        workflow.command("command-2");

        assertThat(activities.secondApplied.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(activities.invocations.get()).isEqualTo(2);
    }

    private static final class CountingActivities implements TemporalWorkflowContracts.ControllerActivities {
        private final AtomicInteger invocations = new AtomicInteger();
        private final CountDownLatch firstApplied = new CountDownLatch(1);
        private final CountDownLatch secondApplied = new CountDownLatch(1);
        private volatile boolean failFirst;

        @Override public String acquireLease(String leaseKey, String ownerId) { return ownerId; }
        @Override public String heartbeatLease(String leaseKey, String ownerId) { return ownerId; }
        @Override public String releaseLease(String leaseKey, String ownerId) { return ownerId; }
        @Override public String recordEvent(String ownerId, String eventType) { return eventType; }
        @Override public String executeAgentRun(String runId) { return runId; }
        @Override public String executeSprint(String sprintId) { return sprintId; }
        @Override public String captureCheckpoint(String runId) { return runId; }
        @Override public String generateProjectPlan(String projectId) { return projectId; }
        @Override public boolean isAutonomous(String projectId) { return false; }
        @Override public boolean hasRemainingProjectWork(String projectId) { return false; }
        @Override public String setProjectManagerStatus(String projectId, String status, String detail) { return status; }
        @Override public String approveAndExecuteSprint(String sprintId) { return sprintId; }

        @Override
        public String applySprintCommand(String commandId) {
            int invocation = invocations.incrementAndGet();
            if (invocation == 1) firstApplied.countDown();
            if (failFirst && invocation == 1) {
                throw new IllegalStateException("transient activity failure");
            }
            if (invocation > 1) secondApplied.countDown();
            return commandId;
        }
    }
}
