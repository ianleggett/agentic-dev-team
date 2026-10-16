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
@Table(name = "project_manager_planning_attempts")
public class ProjectManagerPlanningAttempt {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(nullable = false, length = 40) private String state;
    @Column(nullable = false, columnDefinition = "text") private String prompt;
    @Column(name = "model_output", columnDefinition = "text") private String modelOutput;
    @Column(name = "provider_identifier") private String providerIdentifier;
    @Column(name = "model_identifier") private String modelIdentifier;
    @Column(name = "base_url") private String baseUrl;
    @Column(name = "error_code") private String errorCode;
    @Column(name = "error_message", columnDefinition = "text") private String errorMessage;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "finished_at") private Instant finishedAt;

    protected ProjectManagerPlanningAttempt() { }
    public ProjectManagerPlanningAttempt(UUID projectId, String prompt, String providerIdentifier,
            String modelIdentifier, String baseUrl, Instant createdAt) {
        this.projectId = projectId; this.prompt = prompt; this.providerIdentifier = providerIdentifier;
        this.modelIdentifier = modelIdentifier; this.baseUrl = baseUrl; this.state = "RUNNING"; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public String getState() { return state; }
    public String getPrompt() { return prompt; }
    public String getModelOutput() { return modelOutput; }
    public String getProviderIdentifier() { return providerIdentifier; }
    public String getModelIdentifier() { return modelIdentifier; }
    public String getBaseUrl() { return baseUrl; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void finish(String state, String output, String errorCode, String errorMessage, Instant finishedAt) {
        this.state = state; this.modelOutput = output; this.errorCode = errorCode; this.errorMessage = errorMessage; this.finishedAt = finishedAt;
    }
}
