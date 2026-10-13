package dev.yaadt.controller.manager;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ManagerAssignmentRequest(@NotNull UUID agentProfileVersionId) { }
