package dev.yaadt.controller.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "repositories")
public class Repository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "remote_url", nullable = false, length = 500)
    private String remoteUrl;

    @Column(name = "default_branch", nullable = false, length = 120)
    private String defaultBranch;

    @Column(nullable = false, length = 40)
    private String role;

    @Column(name = "relative_workspace_path", nullable = false, length = 240)
    private String relativeWorkspacePath;

    @Column(name = "build_commands", nullable = false, columnDefinition = "text")
    private String buildCommands;

    @Column(name = "test_commands", nullable = false, columnDefinition = "text")
    private String testCommands;

    @Column(name = "health_check", columnDefinition = "text")
    private String healthCheck;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Repository() {
    }

    public Repository(
            UUID projectId,
            String name,
            String remoteUrl,
            String defaultBranch,
            String role,
            String relativeWorkspacePath,
            String buildCommands,
            String testCommands,
            String healthCheck,
            Instant createdAt) {
        this.projectId = projectId;
        this.name = name;
        this.remoteUrl = remoteUrl;
        this.defaultBranch = defaultBranch;
        this.role = role;
        this.relativeWorkspacePath = relativeWorkspacePath;
        this.buildCommands = buildCommands;
        this.testCommands = testCommands;
        this.healthCheck = healthCheck;
        this.active = true;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public String getRole() {
        return role;
    }

    public String getRelativeWorkspacePath() {
        return relativeWorkspacePath;
    }

    public String getBuildCommands() {
        return buildCommands;
    }

    public String getTestCommands() {
        return testCommands;
    }

    public String getHealthCheck() {
        return healthCheck;
    }

    public boolean isActive() {
        return active;
    }

    public void update(RepositoryRequest request, Instant now) {
        this.name = request.name();
        this.remoteUrl = request.remoteUrl();
        this.defaultBranch = request.defaultBranch();
        this.role = request.role();
        this.relativeWorkspacePath = request.relativeWorkspacePath();
        this.buildCommands = request.buildCommands();
        this.testCommands = request.testCommands();
        this.healthCheck = request.healthCheck();
        this.active = request.active() == null || request.active();
        this.updatedAt = now;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
