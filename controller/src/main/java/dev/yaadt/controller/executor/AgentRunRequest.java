package dev.yaadt.controller.executor;

import java.util.Map;
import java.util.List;
import java.util.UUID;

public record AgentRunRequest(String idempotencyKey, UUID stepId, int attemptNumber,
        String workspaceId, String promptReference, Map<String, String> environment, AgentBackendConfig backend,
        List<String> beadIds, String buildCommands, String testCommands) {
    public AgentRunRequest(String idempotencyKey, UUID stepId, int attemptNumber,
            String workspaceId, String promptReference, Map<String, String> environment) {
        this(idempotencyKey, stepId, attemptNumber, workspaceId, promptReference, environment, null, List.of(), "", "");
    }
    public AgentRunRequest(String idempotencyKey, UUID stepId, int attemptNumber,
            String workspaceId, String promptReference, Map<String, String> environment, AgentBackendConfig backend) {
        this(idempotencyKey, stepId, attemptNumber, workspaceId, promptReference, environment, backend, List.of(), "", "");
    }
    public AgentRunRequest {
        environment = environment == null ? Map.of() : Map.copyOf(environment);
        beadIds = beadIds == null ? List.of() : List.copyOf(beadIds);
        buildCommands = buildCommands == null ? "" : buildCommands;
        testCommands = testCommands == null ? "" : testCommands;
    }
}
