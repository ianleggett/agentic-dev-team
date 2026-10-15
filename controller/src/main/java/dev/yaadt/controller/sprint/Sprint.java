package dev.yaadt.controller.sprint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sprints")
public class Sprint {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(nullable = false, length = 500) private String objective;
    @Column(nullable = false, length = 40) private String state;
    @Column(name = "current_plan_version", nullable = false) private int currentPlanVersion;
    @Column(name = "created_by", nullable = false) private UUID createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Sprint() { }
    public Sprint(UUID projectId, String objective, UUID createdBy, Instant now) {
        this.projectId = projectId; this.objective = objective; this.state = "DRAFT";
        this.currentPlanVersion = 1; this.createdBy = createdBy; this.createdAt = now; this.updatedAt = now;
    }
    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public String getObjective() { return objective; }
    public String getState() { return state; }
    public int getCurrentPlanVersion() { return currentPlanVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void transition(String next, Instant now) { this.state = next; this.updatedAt = now; }
    public void replan(int version, String nextObjective, Instant now) {
        this.currentPlanVersion = version;
        this.objective = nextObjective;
        this.state = "DRAFT";
        this.updatedAt = now;
    }
}
