package dev.yaadt.controller.executor;

import java.util.Map;
import java.util.UUID;

public record AgentRunRequest(String idempotencyKey, UUID stepId, int attemptNumber,
        String workspaceId, String promptReference, Map<String, String> environment) {
    public AgentRunRequest {
        environment = environment == null ? Map.of() : Map.copyOf(environment);
    }
}
