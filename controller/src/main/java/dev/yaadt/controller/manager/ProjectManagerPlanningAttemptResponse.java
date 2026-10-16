package dev.yaadt.controller.manager;

import java.time.Instant;
import java.util.UUID;

public record ProjectManagerPlanningAttemptResponse(UUID id, UUID projectId, String state, String prompt,
        String modelOutput, String providerIdentifier, String modelIdentifier, String baseUrl,
        String errorCode, String errorMessage, Instant createdAt, Instant finishedAt) {
    public static ProjectManagerPlanningAttemptResponse from(ProjectManagerPlanningAttempt attempt) {
        return new ProjectManagerPlanningAttemptResponse(attempt.getId(), attempt.getProjectId(), attempt.getState(),
                attempt.getPrompt(), attempt.getModelOutput(), attempt.getProviderIdentifier(), attempt.getModelIdentifier(),
                attempt.getBaseUrl(), attempt.getErrorCode(), attempt.getErrorMessage(), attempt.getCreatedAt(), attempt.getFinishedAt());
    }
}
