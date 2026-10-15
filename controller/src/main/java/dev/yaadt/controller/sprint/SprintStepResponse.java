package dev.yaadt.controller.sprint;

import java.util.UUID;

public record SprintStepResponse(UUID id, String stepKey, String agentRole, UUID agentProfileVersionId,
        String state, String successCriteria) {
    public static SprintStepResponse from(SprintStep step) {
        return new SprintStepResponse(step.getId(), step.getStepKey(), step.getAgentRole(),
                step.getAgentProfileVersionId(), step.getState(), step.getSuccessCriteria());
    }
}
