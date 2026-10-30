package dev.yaadt.controller.beads;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "beads_mutation_outbox")
public class BeadsMutationOutbox {
    @Id private UUID id;
    @Column(name = "sprint_id", nullable = false) private UUID sprintId;
    @Column(name = "mutation_key", nullable = false, unique = true, length = 240) private String mutationKey;
    @Column(name = "bead_id", nullable = false, length = 120) private String beadId;
    @Column(nullable = false, length = 40) private String operation;
    @Column(nullable = false, columnDefinition = "text") private String reason;
    @Column(nullable = false, length = 20) private String status;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "last_error", columnDefinition = "text") private String lastError;

    protected BeadsMutationOutbox() { }

    public BeadsMutationOutbox(UUID sprintId, String mutationKey, String beadId, String operation, String reason, Instant now) {
        this.id = UUID.randomUUID(); this.sprintId = sprintId; this.mutationKey = mutationKey; this.beadId = beadId; this.operation = operation;
        this.reason = reason; this.status = "PENDING"; this.nextAttemptAt = now;
    }

    public UUID getId() { return id; }
    public UUID getSprintId() { return sprintId; }
    public String getMutationKey() { return mutationKey; }
    public String getBeadId() { return beadId; }
    public String getOperation() { return operation; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getLastError() { return lastError; }
    public void delivered() { status = "DELIVERED"; }
    public void retry(String error, Instant next) { attempts++; lastError = error; nextAttemptAt = next; }
}
