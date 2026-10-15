package dev.yaadt.controller.executor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "run_events")
public class RunEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "agent_run_id", nullable = false) private UUID agentRunId;
    @Column(name = "sequence_number", nullable = false) private long sequenceNumber;
    @Column(name = "event_type", nullable = false, length = 80) private String eventType;
    @Column(nullable = false, columnDefinition = "text") private String payload;
    @Column(nullable = false) private boolean redacted;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected RunEvent() { }
    public RunEvent(UUID agentRunId, AgentRunEvent event) {
        this.agentRunId = agentRunId; this.sequenceNumber = event.sequenceNumber(); this.eventType = event.eventType();
        this.payload = event.payload(); this.redacted = event.redacted(); this.createdAt = event.createdAt();
    }
    public UUID getId() { return id; }
    public UUID getAgentRunId() { return agentRunId; }
    public long getSequenceNumber() { return sequenceNumber; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public boolean isRedacted() { return redacted; }
    public Instant getCreatedAt() { return createdAt; }
}
