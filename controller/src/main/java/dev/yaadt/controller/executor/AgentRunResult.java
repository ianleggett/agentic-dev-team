package dev.yaadt.controller.executor;

import java.util.List;

public record AgentRunResult(String idempotencyKey, State state, String commitSha,
        String errorCode, String errorMessage, List<AgentRunEvent> events) {
    public enum State { SUCCEEDED, FAILED, TIMED_OUT }
    public AgentRunResult {
        events = events == null ? List.of() : List.copyOf(events);
    }
}
