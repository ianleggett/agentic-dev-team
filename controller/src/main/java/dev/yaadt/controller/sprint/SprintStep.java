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
@Table(name = "sprint_steps")
public class SprintStep {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "plan_version_id", nullable = false) private UUID planVersionId;
    @Column(name = "step_key", nullable = false, length = 120) private String stepKey;
    @Column(name = "bead_ids", nullable = false, columnDefinition = "text") private String beadIds;
    @Column(name = "agent_role", nullable = false, length = 60) private String agentRole;
    @Column(name = "agent_profile_version_id", nullable = false) private UUID agentProfileVersionId;
    @Column(name = "repository_ids", nullable = false, columnDefinition = "text") private String repositoryIds;
    @Column(nullable = false, length = 40) private String state;
    @Column(name = "success_criteria", nullable = false, columnDefinition = "text") private String successCriteria;
    @Column(name = "failure_policy", nullable = false, columnDefinition = "text") private String failurePolicy;
    @Column(name = "dependency_keys", nullable = false, columnDefinition = "text") private String dependencyKeys;
    @Column(name = "resource_locks", nullable = false, columnDefinition = "text") private String resourceLocks;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected SprintStep() { }
    public SprintStep(UUID planId, SprintStepRequest request, Instant now) {
        this.planVersionId = planId; this.stepKey = request.stepKey(); this.beadIds = String.join(",", request.beadIds());
        this.agentRole = request.agentRole(); this.agentProfileVersionId = request.agentProfileVersionId();
        this.repositoryIds = request.repositoryIds().toString(); this.state = "PENDING";
        this.successCriteria = request.successCriteria(); this.failurePolicy = request.failurePolicy();
        this.dependencyKeys = String.join(",", request.dependsOn()); this.resourceLocks = String.join(",", request.resourceLocks());
        this.createdAt = now; this.updatedAt = now;
    }
    public UUID getId() { return id; }
    public UUID getPlanVersionId() { return planVersionId; }
    public String getStepKey() { return stepKey; }
    public String getBeadIds() { return beadIds; }
    public String getAgentRole() { return agentRole; }
    public UUID getAgentProfileVersionId() { return agentProfileVersionId; }
    public String getRepositoryIds() { return repositoryIds; }
    public String getState() { return state; }
    public String getSuccessCriteria() { return successCriteria; }
    public String getFailurePolicy() { return failurePolicy; }
    public String getDependencyKeys() { return dependencyKeys; }
    public String getResourceLocks() { return resourceLocks; }
}
