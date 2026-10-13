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
@Table(name = "sprint_plan_versions")
public class SprintPlanVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "sprint_id", nullable = false) private UUID sprintId;
    @Column(nullable = false) private int version;
    @Column(name = "directive_revision_id", nullable = false) private UUID directiveRevisionId;
    @Column(nullable = false, columnDefinition = "text") private String reasoning;
    @Column(name = "approval_policy", nullable = false, columnDefinition = "text") private String approvalPolicy;
    @Column(name = "failure_policy", nullable = false, columnDefinition = "text") private String failurePolicy;
    @Column(name = "created_by", nullable = false, length = 80) private String createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected SprintPlanVersion() { }
    public SprintPlanVersion(UUID sprintId, int version, SprintPlanRequest request, String createdBy, Instant now) {
        this.sprintId = sprintId; this.version = version; this.directiveRevisionId = request.directiveRevisionId();
        this.reasoning = request.reasoning(); this.approvalPolicy = request.approvalPolicy();
        this.failurePolicy = request.failurePolicy(); this.createdBy = createdBy; this.createdAt = now;
    }
    public UUID getId() { return id; }
    public UUID getSprintId() { return sprintId; }
    public int getVersion() { return version; }
    public UUID getDirectiveRevisionId() { return directiveRevisionId; }
    public String getReasoning() { return reasoning; }
    public String getApprovalPolicy() { return approvalPolicy; }
    public String getFailurePolicy() { return failurePolicy; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
