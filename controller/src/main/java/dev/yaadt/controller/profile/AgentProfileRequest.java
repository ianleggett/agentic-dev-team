package dev.yaadt.controller.profile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AgentProfileRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 60) String role,
        @NotBlank @Size(max = 160) String modelIdentifier,
        @NotBlank @Size(max = 120) String providerIdentifier,
        @NotBlank @Size(max = 240) String promptReference,
        @NotNull String toolPolicy,
        @NotNull String permissionPolicy,
        @NotNull String environmentPolicy,
        @Min(1) int maxSteps,
        @Min(1) int timeoutSeconds) {
}
