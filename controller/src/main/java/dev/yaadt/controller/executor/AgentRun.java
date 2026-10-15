package dev.yaadt.controller.executor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_runs")
public class AgentRun {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "step_id", nullable = false) private UUID stepId;
    @Column(name = "attempt_number", nullable = false) private int attemptNumber;
    @Column(name = "idempotency_key", nullable = false, unique = true) private String idempotencyKey;
    @Column(nullable = false, length = 40) private String state;
    @Column(name = "executor_id") private String executorId;
    @Column(name = "workspace_id", nullable = false) private String workspaceId;
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "heartbeat_at") private Instant heartbeatAt;
    @Column(name = "finished_at") private Instant finishedAt;
    @Column(name = "error_code") private String errorCode;
    @Column(name = "error_message", columnDefinition = "text") private String errorMessage;
    @Column(name = "checkpoint_sha", length = 64) private String checkpointSha;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected AgentRun() { }
    public AgentRun(UUID stepId, int attemptNumber, String idempotencyKey, String workspaceId, Instant now) {
        this.stepId = stepId; this.attemptNumber = attemptNumber; this.idempotencyKey = idempotencyKey;
        this.state = "CREATED"; this.workspaceId = workspaceId; this.createdAt = now;
    }
    public void start(Instant now) { state = "RUNNING"; startedAt = now; heartbeatAt = now; }
    public void finish(AgentRunResult result, Instant now) {
        state = switch (result.state()) { case SUCCEEDED -> "SUCCEEDED"; case FAILED -> "FAILED"; case TIMED_OUT -> "TIMED_OUT"; };
        finishedAt = now; errorCode = result.errorCode(); errorMessage = result.errorMessage();
        checkpointSha = result.commitSha();
    }
    public UUID getId() { return id; }
    public UUID getStepId() { return stepId; }
    public int getAttemptNumber() { return attemptNumber; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getState() { return state; }
    public String getExecutorId() { return executorId; }
    public String getWorkspaceId() { return workspaceId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getHeartbeatAt() { return heartbeatAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public String getCheckpointSha() { return checkpointSha; }
    public Instant getCreatedAt() { return createdAt; }
}
