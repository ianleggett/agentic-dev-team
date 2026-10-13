package dev.yaadt.controller.sprint;
import java.time.Instant;
import java.util.UUID;
public record SprintResponse(UUID id, UUID projectId, String objective, String state, int planVersion,
        UUID planVersionId, int stepCount, Instant createdAt, Instant updatedAt) { }
