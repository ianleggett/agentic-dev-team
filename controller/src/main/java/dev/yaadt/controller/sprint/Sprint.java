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
    @Column(name = "temporal_workflow_id", length = 240) private String temporalWorkflowId;
    @Column(name = "temporal_run_id", length = 240) private String temporalRunId;
    @Column(name = "state_version", nullable = false) private long stateVersion;
    @Column(name = "recovery_reason", columnDefinition = "text") private String recoveryReason;
    @Column(name = "delivery_plan_id") private UUID deliveryPlanId;
    @Column(name = "delivery_plan_version_id") private UUID deliveryPlanVersionId;
    @Column(name = "delivery_milestone_key", length = 120) private String deliveryMilestoneKey;
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
    public String getTemporalWorkflowId() { return temporalWorkflowId; }
    public String getTemporalRunId() { return temporalRunId; }
    public long getStateVersion() { return stateVersion; }
    public String getRecoveryReason() { return recoveryReason; }
    public UUID getDeliveryPlanId() { return deliveryPlanId; }
    public UUID getDeliveryPlanVersionId() { return deliveryPlanVersionId; }
    public String getDeliveryMilestoneKey() { return deliveryMilestoneKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void transition(String next, Instant now) { this.state = next; this.stateVersion++; this.updatedAt = now; }
    public void bindTemporalExecution(String workflowId, String runId, Instant now) {
        this.temporalWorkflowId = workflowId; this.temporalRunId = runId; this.updatedAt = now;
    }
    public void markRecoveryRequired(String reason, Instant now) {
        this.state = "RECOVERY_REQUIRED"; this.recoveryReason = reason; this.stateVersion++; this.updatedAt = now;
    }
    public void bindDeliveryPlan(UUID planId, UUID versionId, String milestoneKey, Instant now) {
        this.deliveryPlanId = planId; this.deliveryPlanVersionId = versionId; this.deliveryMilestoneKey = milestoneKey; this.updatedAt = now;
    }
    public void replan(int version, String nextObjective, Instant now) {
        this.currentPlanVersion = version;
        this.objective = nextObjective;
        this.state = "DRAFT";
        this.updatedAt = now;
    }
}
