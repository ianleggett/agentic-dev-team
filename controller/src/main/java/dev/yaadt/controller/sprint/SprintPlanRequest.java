package dev.yaadt.controller.sprint;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SprintPlanRequest(
        @NotBlank @Size(max = 500) String objective,
        @NotNull UUID directiveRevisionId,
        @NotBlank String reasoning,
        @NotBlank String approvalPolicy,
        @NotBlank String failurePolicy,
        @NotEmpty List<@Valid SprintStepRequest> steps) { }
