package dev.yaadt.controller.sprint;

import java.util.UUID;
import java.util.List;
import dev.yaadt.controller.executor.AgentRunResponse;

public record SprintStepResponse(UUID id, String stepKey, String agentRole, UUID agentProfileVersionId,
        String state, String successCriteria, List<String> beadIds, List<AgentRunResponse> runs) {
    public static SprintStepResponse from(SprintStep step, List<AgentRunResponse> runs) {
        return new SprintStepResponse(step.getId(), step.getStepKey(), step.getAgentRole(),
                step.getAgentProfileVersionId(), step.getState(), step.getSuccessCriteria(),
                step.getBeadIds().isBlank() ? List.of() : List.of(step.getBeadIds().split(",")), runs);
    }
}
