package dev.yaadt.controller.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(UUID id, String actorType, String actorId, UUID sprintId,
        String eventType, String fromState, String toState, String payload, Instant createdAt) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getActorType(), event.getActorId(), event.getSprintId(),
                event.getEventType(), event.getFromState(), event.getToState(), event.getPayload(), event.getCreatedAt());
    }
}
