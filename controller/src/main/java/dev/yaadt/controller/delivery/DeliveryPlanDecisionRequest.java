package dev.yaadt.controller.delivery;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DeliveryPlanDecisionRequest(@Min(1) int version, @NotBlank String reason) { }
