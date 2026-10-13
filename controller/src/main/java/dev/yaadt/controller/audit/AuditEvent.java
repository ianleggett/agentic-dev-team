package dev.yaadt.controller.audit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name = "audit_events")
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "actor_type", nullable = false) private String actorType;
    @Column(name = "actor_id", nullable = false) private String actorId;
    @Column(name = "sprint_id") private UUID sprintId;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(name = "from_state") private String fromState;
    @Column(name = "to_state") private String toState;
    @Column(nullable = false, columnDefinition = "text") private String payload;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected AuditEvent() { }
    public AuditEvent(String actorId, UUID sprintId, String eventType, String fromState, String toState, String payload, Instant createdAt) {
        this.actorType = "USER"; this.actorId = actorId; this.sprintId = sprintId; this.eventType = eventType;
        this.fromState = fromState; this.toState = toState; this.payload = payload; this.createdAt = createdAt;
    }
}
