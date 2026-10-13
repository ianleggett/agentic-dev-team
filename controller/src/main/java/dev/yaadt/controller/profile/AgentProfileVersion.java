package dev.yaadt.controller.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_profile_versions")
public class AgentProfileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(nullable = false)
    private int version;

    @Column(name = "model_identifier", nullable = false, length = 160)
    private String modelIdentifier;

    @Column(name = "provider_identifier", nullable = false, length = 120)
    private String providerIdentifier;

    @Column(name = "prompt_reference", nullable = false, length = 240)
    private String promptReference;

    @Column(name = "tool_policy", nullable = false, columnDefinition = "text")
    private String toolPolicy;

    @Column(name = "permission_policy", nullable = false, columnDefinition = "text")
    private String permissionPolicy;

    @Column(name = "environment_policy", nullable = false, columnDefinition = "text")
    private String environmentPolicy;

    @Column(name = "max_steps", nullable = false)
    private int maxSteps;

    @Column(name = "timeout_seconds", nullable = false)
    private int timeoutSeconds;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AgentProfileVersion() {
    }

    public AgentProfileVersion(
            UUID profileId, int version, AgentProfileRequest request, UUID createdBy, Instant createdAt) {
        this.profileId = profileId;
        this.version = version;
        this.modelIdentifier = request.modelIdentifier();
        this.providerIdentifier = request.providerIdentifier();
        this.promptReference = request.promptReference();
        this.toolPolicy = request.toolPolicy();
        this.permissionPolicy = request.permissionPolicy();
        this.environmentPolicy = request.environmentPolicy();
        this.maxSteps = request.maxSteps();
        this.timeoutSeconds = request.timeoutSeconds();
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getProfileId() { return profileId; }
    public int getVersion() { return version; }
    public String getModelIdentifier() { return modelIdentifier; }
    public String getProviderIdentifier() { return providerIdentifier; }
    public String getPromptReference() { return promptReference; }
    public String getToolPolicy() { return toolPolicy; }
    public String getPermissionPolicy() { return permissionPolicy; }
    public String getEnvironmentPolicy() { return environmentPolicy; }
    public int getMaxSteps() { return maxSteps; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
