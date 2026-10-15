package dev.yaadt.controller.executor;

import java.time.Instant;
import java.util.UUID;

public record RunEventResponse(UUID id, UUID agentRunId, long sequenceNumber, String eventType,
        String payload, boolean redacted, Instant createdAt) {
    public static RunEventResponse from(RunEvent event) {
        return new RunEventResponse(event.getId(), event.getAgentRunId(), event.getSequenceNumber(),
                event.getEventType(), event.getPayload(), event.isRedacted(), event.getCreatedAt());
    }
}
