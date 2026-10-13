package dev.yaadt.controller.project;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 120) String beadsProject,
        @NotBlank @Size(max = 40) String workspaceProvider,
        Boolean active,
        @Size(max = 120) String defaultBranch,
        @Size(max = 32) String executionMode,
        @Min(1) Integer maxConcurrentAgentRuns,
        @Min(1) Integer maxConcurrentRunsPerProject,
        Boolean approvalRequired) {
}
