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
@Table(name = "sprint_command_idempotency")
public class SprintCommandIdempotency {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 240) private String key;
    @Column(name = "sprint_id", nullable = false) private UUID sprintId;
    @Column(nullable = false, length = 40) private String action;
    @Column(name = "plan_version", nullable = false) private int planVersion;
    @Column(nullable = false, columnDefinition = "text") private String reason;
    @Column(name = "result_state", nullable = false, length = 40) private String resultState;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected SprintCommandIdempotency() { }
    public SprintCommandIdempotency(String key, UUID sprintId, String action, int planVersion, String reason, String resultState, Instant createdAt) {
        this.key = key; this.sprintId = sprintId; this.action = action; this.planVersion = planVersion;
        this.reason = reason; this.resultState = resultState; this.createdAt = createdAt;
    }
    public UUID getSprintId() { return sprintId; }
    public String getAction() { return action; }
    public int getPlanVersion() { return planVersion; }
    public String getReason() { return reason; }
}
