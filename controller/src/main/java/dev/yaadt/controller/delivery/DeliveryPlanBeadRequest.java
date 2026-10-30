package dev.yaadt.controller.delivery;

import jakarta.validation.constraints.NotBlank;

public record DeliveryPlanBeadRequest(
        @NotBlank String beadId,
        String milestoneKey,
        @NotBlank String titleSnapshot,
        @NotBlank String statusSnapshot,
        String dependencyIdsSnapshot,
        String labelsSnapshot,
        @NotBlank String beadSnapshot) {
    public DeliveryPlanBeadRequest {
        dependencyIdsSnapshot = dependencyIdsSnapshot == null ? "[]" : dependencyIdsSnapshot;
        labelsSnapshot = labelsSnapshot == null ? "[]" : labelsSnapshot;
    }
}
