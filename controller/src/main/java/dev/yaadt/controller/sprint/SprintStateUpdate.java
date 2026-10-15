package dev.yaadt.controller.sprint;

import java.time.Instant;
import java.util.UUID;

public record SprintStateUpdate(UUID id, String state, Instant updatedAt) { }
