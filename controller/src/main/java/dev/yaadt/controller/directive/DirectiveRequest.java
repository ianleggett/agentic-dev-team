package dev.yaadt.controller.directive;

import jakarta.validation.constraints.NotBlank;

public record DirectiveRequest(@NotBlank String content) { }
