package dev.yaadt.controller.delivery;

import jakarta.validation.constraints.NotBlank;

public record DeliveryMilestoneRequest(
        @NotBlank String milestoneKey,
        @NotBlank String title,
        @NotBlank String outcome,
        int sequenceNumber) { }
