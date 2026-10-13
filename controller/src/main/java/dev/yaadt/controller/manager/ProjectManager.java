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
}
