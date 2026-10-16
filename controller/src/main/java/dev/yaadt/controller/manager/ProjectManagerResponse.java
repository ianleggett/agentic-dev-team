package dev.yaadt.controller.manager;

import java.time.Instant;
import java.util.UUID;

public record ProjectManagerResponse(UUID id, UUID projectId, UUID agentProfileVersionId,
        String temporalWorkflowId, String state, Instant updatedAt, String lastPlanningError, Instant lastPlanningFailedAt,
        String planningObjective) {
    public static ProjectManagerResponse from(ProjectManager manager) {
        return new ProjectManagerResponse(manager.getId(), manager.getProjectId(), manager.getAgentProfileVersionId(),
                manager.getTemporalWorkflowId(), manager.getState(), manager.getUpdatedAt(), manager.getLastPlanningError(), manager.getLastPlanningFailedAt(), manager.getPlanningObjective());
    }
}
