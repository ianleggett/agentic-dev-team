package dev.yaadt.controller.manager;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_managers")
public class ProjectManager {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "project_id", nullable = false, unique = true) private UUID projectId;
    @Column(name = "agent_profile_version_id", nullable = false) private UUID agentProfileVersionId;
    @Column(name = "temporal_workflow_id", unique = true, length = 240) private String temporalWorkflowId;
    @Column(nullable = false, length = 40) private String state;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "last_planning_error", columnDefinition = "text") private String lastPlanningError;
    @Column(name = "last_planning_failed_at") private Instant lastPlanningFailedAt;
    @Column(name = "planning_objective", length = 500) private String planningObjective;
    protected ProjectManager() { }
    public ProjectManager(UUID projectId, UUID profileVersionId, Instant updatedAt) {
        this.projectId = projectId; this.agentProfileVersionId = profileVersionId;
         this.state = "ENABLED"; this.updatedAt = updatedAt;
    }
    public void assign(UUID profileVersionId, Instant updatedAt) {
        this.agentProfileVersionId = profileVersionId; this.state = "ENABLED"; this.updatedAt = updatedAt;
    }
    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public UUID getAgentProfileVersionId() { return agentProfileVersionId; }
    public String getTemporalWorkflowId() { return temporalWorkflowId; }
    public String getState() { return state; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getLastPlanningError() { return lastPlanningError; }
    public Instant getLastPlanningFailedAt() { return lastPlanningFailedAt; }
    public String getPlanningObjective() { return planningObjective; }
    public void setPlanningObjective(String objective, Instant now) { planningObjective = objective; updatedAt = now; }
    public void clearPlanningFailure(Instant now) { lastPlanningError = null; lastPlanningFailedAt = null; updatedAt = now; }
    public void recordPlanningFailure(String error, Instant now) {
        lastPlanningError = error == null ? "Unknown planning failure" : error.substring(0, Math.min(error.length(), 4000));
        lastPlanningFailedAt = now; updatedAt = now;
    }
}
