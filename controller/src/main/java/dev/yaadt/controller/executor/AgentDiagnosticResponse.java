package dev.yaadt.controller.executor;

import java.util.List;
import java.util.UUID;

public record AgentDiagnosticResponse(
        String state,
        UUID agentProfileVersionId,
        String provider,
        String model,
        String llmUrl,
        String errorCode,
        String errorMessage,
        List<AgentRunEvent> events) {
}
