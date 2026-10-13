package dev.yaadt.controller.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "beads_project", nullable = false, length = 120)
    private String beadsProject;

    @Column(name = "workspace_provider", nullable = false, length = 40)
    private String workspaceProvider;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "default_branch", nullable = false, length = 120)
    private String defaultBranch;

    @Column(name = "execution_mode", nullable = false, length = 32)
    private String executionMode;

    @Column(name = "max_concurrent_agent_runs", nullable = false)
    private int maxConcurrentAgentRuns;

    @Column(name = "max_concurrent_runs_per_project", nullable = false)
    private int maxConcurrentRunsPerProject;

    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired;

    protected Project() {
    }

    public Project(String name, String beadsProject, String workspaceProvider) {
        this.name = name;
        this.beadsProject = beadsProject;
        this.workspaceProvider = workspaceProvider;
        this.active = true;
        this.defaultBranch = "main";
        this.executionMode = "GLOBAL_SERIAL";
        this.maxConcurrentAgentRuns = 1;
        this.maxConcurrentRunsPerProject = 1;
        this.approvalRequired = true;
    }

    public void update(
            String name,
            String beadsProject,
            String workspaceProvider,
            boolean active,
            String defaultBranch,
            String executionMode,
            int maxConcurrentAgentRuns,
            int maxConcurrentRunsPerProject,
            boolean approvalRequired) {
        this.name = name;
        this.beadsProject = beadsProject;
        this.workspaceProvider = workspaceProvider;
        this.active = active;
        this.defaultBranch = defaultBranch;
        this.executionMode = executionMode;
        this.maxConcurrentAgentRuns = maxConcurrentAgentRuns;
        this.maxConcurrentRunsPerProject = maxConcurrentRunsPerProject;
        this.approvalRequired = approvalRequired;
    }

    public void touch(Instant now) {
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBeadsProject() {
        return beadsProject;
    }

    public String getWorkspaceProvider() {
        return workspaceProvider;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public int getMaxConcurrentAgentRuns() {
        return maxConcurrentAgentRuns;
    }

    public int getMaxConcurrentRunsPerProject() {
        return maxConcurrentRunsPerProject;
    }

    public boolean isApprovalRequired() {
        return approvalRequired;
    }
}
