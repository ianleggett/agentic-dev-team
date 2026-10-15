package dev.yaadt.controller.sprint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ManualSprintRequest(
        @NotBlank String objective,
        @NotNull UUID directiveRevisionId,
        @NotEmpty List<@NotNull UUID> agentProfileVersionIds,
        @NotEmpty List<@NotNull UUID> repositoryIds,
        @NotBlank String failurePolicy) { }
