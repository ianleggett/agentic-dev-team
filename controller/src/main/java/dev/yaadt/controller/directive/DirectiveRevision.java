package dev.yaadt.controller.directive;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "directive_revisions")
public class DirectiveRevision {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(nullable = false) private int version;
    @Column(nullable = false, columnDefinition = "text") private String content;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_by", nullable = false) private UUID createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected DirectiveRevision() { }
    public DirectiveRevision(UUID projectId, int version, String content, UUID createdBy, Instant createdAt) {
        this.projectId = projectId; this.version = version; this.content = content; this.active = true;
        this.createdBy = createdBy; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public int getVersion() { return version; }
    public String getContent() { return content; }
    public boolean isActive() { return active; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
