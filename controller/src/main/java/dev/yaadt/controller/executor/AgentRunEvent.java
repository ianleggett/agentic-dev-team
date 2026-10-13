package dev.yaadt.controller.executor;

import java.time.Instant;

public record AgentRunEvent(long sequenceNumber, String eventType, String payload, boolean redacted, Instant createdAt) { }
