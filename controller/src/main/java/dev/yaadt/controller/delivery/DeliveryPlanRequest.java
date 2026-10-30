package dev.yaadt.controller.delivery;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record DeliveryPlanRequest(
        @NotBlank String objective,
        @NotBlank String reasoning,
        @NotBlank String acceptanceCriteria,
        @NotBlank String suggestedSprintBoundaries,
        @NotBlank String planSnapshot,
        @Valid List<DeliveryMilestoneRequest> milestones,
        @NotEmpty @Valid List<DeliveryPlanBeadRequest> beads,
        @NotNull UUID directiveRevisionId) { }
