package dev.yaadt.controller.repository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RepositoryRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 500) String remoteUrl,
        @NotBlank @Size(max = 120) String defaultBranch,
        @NotBlank @Size(max = 40) String role,
        @NotBlank @Size(max = 240)
        @Pattern(regexp = "\\.|[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*(?:/[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*)*",
                message = "relativeWorkspacePath must contain only safe relative path segments")
        String relativeWorkspacePath,
        @NotBlank String buildCommands,
        @NotBlank String testCommands,
        String healthCheck,
        Boolean active) {
}
