package dev.yaadt.controller.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_plans")
public class DeliveryPlan {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(nullable = false, length = 500) private String objective;
    @Column(nullable = false, length = 40) private String state;
    @Column(name = "current_version", nullable = false) private int currentVersion;
    @Column(name = "approved_version") private Integer approvedVersion;
    @Column(name = "approved_by", length = 240) private String approvedBy;
    @Column(name = "approved_at") private Instant approvedAt;
    @Column(name = "approval_reason", columnDefinition = "text") private String approvalReason;
    @Column(name = "created_by", nullable = false) private UUID createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected DeliveryPlan() { }

    public DeliveryPlan(UUID projectId, String objective, UUID createdBy, Instant now) {
        this.projectId = projectId;
        this.objective = objective;
        this.state = "DRAFT";
        this.currentVersion = 1;
        this.createdBy = createdBy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void submitForApproval(Instant now) { state = "PENDING_APPROVAL"; updatedAt = now; }
    public void approve(int version, String approver, String reason, Instant now) {
        if (version != currentVersion) throw new IllegalArgumentException("Only the current plan version can be approved");
        state = "APPROVED"; approvedVersion = version; approvedBy = approver; approvedAt = now; approvalReason = reason; updatedAt = now;
    }
    public void reject(Instant now) { state = "REJECTED"; updatedAt = now; }
    public void supersede(Instant now) { state = "SUPERSEDED"; updatedAt = now; }
    public int revise(String nextObjective, Instant now) {
        currentVersion++;
        objective = nextObjective;
        approvedVersion = null; approvedBy = null; approvedAt = null; approvalReason = null;
        state = "DRAFT";
        updatedAt = now;
        return currentVersion;
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public String getObjective() { return objective; }
    public String getState() { return state; }
    public int getCurrentVersion() { return currentVersion; }
    public Integer getApprovedVersion() { return approvedVersion; }
    public String getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public String getApprovalReason() { return approvalReason; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
