package dev.yaadt.controller.executor;

import java.time.Instant;
import java.util.UUID;

public record AgentRunResponse(UUID id, UUID stepId, int attemptNumber, String state, String workspaceId,
        Instant startedAt, Instant finishedAt, String checkpointSha, String errorCode, String errorMessage) {
    public static AgentRunResponse from(AgentRun run) {
        return new AgentRunResponse(run.getId(), run.getStepId(), run.getAttemptNumber(), run.getState(),
                run.getWorkspaceId(), run.getStartedAt(), run.getFinishedAt(), run.getCheckpointSha(), run.getErrorCode(), run.getErrorMessage());
    }
}
