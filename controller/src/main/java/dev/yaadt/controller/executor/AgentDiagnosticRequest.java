package dev.yaadt.controller.executor;

import java.util.UUID;

public record AgentDiagnosticRequest(UUID agentProfileVersionId, String prompt) {
}
