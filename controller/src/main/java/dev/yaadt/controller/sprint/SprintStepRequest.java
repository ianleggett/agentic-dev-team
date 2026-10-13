package dev.yaadt.controller.sprint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record SprintStepRequest(
        @NotBlank String stepKey,
        @NotEmpty List<@NotBlank String> beadIds,
        @NotBlank String agentRole,
        @NotNull UUID agentProfileVersionId,
        @NotEmpty List<@NotNull UUID> repositoryIds,
        @NotBlank String successCriteria,
        @NotBlank String failurePolicy,
        List<String> dependsOn,
        List<@NotBlank String> resourceLocks) {
    public SprintStepRequest {
        dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
        resourceLocks = resourceLocks == null ? List.of() : List.copyOf(resourceLocks);
    }
}
