package dev.yaadt.controller.project;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String objective,
        String beadsProject,
        String workspaceProvider,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        String defaultBranch,
        String executionMode,
        int maxConcurrentAgentRuns,
        int maxConcurrentRunsPerProject,
        boolean approvalRequired) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getObjective(),
                project.getBeadsProject(),
                project.getWorkspaceProvider(),
                project.isActive(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getDefaultBranch(),
                project.getExecutionMode(),
                project.getMaxConcurrentAgentRuns(),
                project.getMaxConcurrentRunsPerProject(),
                project.isApprovalRequired());
    }
}
