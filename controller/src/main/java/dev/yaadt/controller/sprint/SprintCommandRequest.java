package dev.yaadt.controller.sprint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record SprintCommandRequest(@NotNull Integer planVersion, @NotBlank String reason) { }
