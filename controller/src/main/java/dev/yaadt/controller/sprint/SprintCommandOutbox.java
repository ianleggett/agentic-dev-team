package dev.yaadt.controller.sprint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sprint_command_outbox")
public class SprintCommandOutbox {
    @Id @Column(name = "command_id") private UUID commandId;
    @Column(nullable = false, length = 20) private String status;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "last_error", columnDefinition = "text") private String lastError;
    protected SprintCommandOutbox() { }
    public SprintCommandOutbox(UUID commandId, Instant now) { this.commandId = commandId; this.status = "PENDING"; this.nextAttemptAt = now; }
    public UUID getCommandId() { return commandId; }
    public int getAttempts() { return attempts; }
    public String getStatus() { return status; }
    public void delivered() { status = "DELIVERED"; }
    public void retry(String error, Instant next) { attempts++; lastError = error; nextAttemptAt = next; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
}
