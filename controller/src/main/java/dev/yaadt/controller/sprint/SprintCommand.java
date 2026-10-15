package dev.yaadt.controller.sprint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sprint_commands")
public class SprintCommand {
    @Id private UUID id;
    @Column(name = "sprint_id", nullable = false) private UUID sprintId;
    @Column(name = "idempotency_key", nullable = false, length = 240) private String idempotencyKey;
    @Column(nullable = false, length = 40) private String action;
    @Column(name = "plan_version", nullable = false) private int planVersion;
    @Column(nullable = false, columnDefinition = "text") private String reason;
    @Column(nullable = false, columnDefinition = "text") private String payload;
    @Column(name = "actor_subject", nullable = false, length = 240) private String actorSubject;
    @Column(name = "actor_name", nullable = false, length = 160) private String actorName;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "result_state", length = 40) private String resultState;
    @Column(name = "error_message", columnDefinition = "text") private String errorMessage;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "applied_at") private Instant appliedAt;
    protected SprintCommand() { }
    public SprintCommand(UUID sprintId, String idempotencyKey, String action, int planVersion, String reason,
            String payload, String actorSubject, String actorName, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.sprintId = sprintId; this.idempotencyKey = idempotencyKey; this.action = action;
        this.planVersion = planVersion; this.reason = reason; this.payload = payload;
        this.actorSubject = actorSubject; this.actorName = actorName; this.status = "RECEIVED"; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getSprintId() { return sprintId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getAction() { return action; }
    public int getPlanVersion() { return planVersion; }
    public String getReason() { return reason; }
    public String getPayload() { return payload; }
    public String getActorSubject() { return actorSubject; }
    public String getActorName() { return actorName; }
    public String getStatus() { return status; }
    public String getResultState() { return resultState; }
    public String getErrorMessage() { return errorMessage; }
    public void applied(String state, Instant now) { status = "APPLIED"; resultState = state; appliedAt = now; }
    public void rejected(String message, Instant now) { status = "REJECTED"; errorMessage = message; appliedAt = now; }
}
