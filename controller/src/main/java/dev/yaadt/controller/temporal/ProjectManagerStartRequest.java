package dev.yaadt.controller.temporal;

import jakarta.validation.constraints.NotBlank;

public record ProjectManagerStartRequest(@NotBlank String objective) { }
