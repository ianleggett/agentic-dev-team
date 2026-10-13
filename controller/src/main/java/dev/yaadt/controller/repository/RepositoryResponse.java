package dev.yaadt.controller.repository;

import java.time.Instant;
import java.util.UUID;

public record RepositoryResponse(
        UUID id,
        UUID projectId,
        String name,
        String remoteUrl,
        String defaultBranch,
        String role,
        String relativeWorkspacePath,
        String buildCommands,
        String testCommands,
        String healthCheck,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static RepositoryResponse from(Repository repository) {
        return new RepositoryResponse(
                repository.getId(), repository.getProjectId(), repository.getName(), repository.getRemoteUrl(),
                repository.getDefaultBranch(), repository.getRole(), repository.getRelativeWorkspacePath(),
                repository.getBuildCommands(), repository.getTestCommands(), repository.getHealthCheck(),
                repository.isActive(), repository.getCreatedAt(), repository.getUpdatedAt());
    }
}
