package dev.yaadt.controller.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_plan_versions")
public class DeliveryPlanVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "delivery_plan_id", nullable = false) private UUID deliveryPlanId;
    @Column(nullable = false) private int version;
    @Column(name = "directive_revision_id") private UUID directiveRevisionId;
    @Column(nullable = false, length = 500) private String objective;
    @Column(nullable = false, columnDefinition = "text") private String reasoning;
    @Column(name = "acceptance_criteria", nullable = false, columnDefinition = "text") private String acceptanceCriteria;
    @Column(name = "suggested_sprint_boundaries", nullable = false, columnDefinition = "text") private String suggestedSprintBoundaries;
    @Column(name = "plan_snapshot", nullable = false, columnDefinition = "text") private String planSnapshot;
    @Column(name = "created_by", nullable = false, length = 80) private String createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected DeliveryPlanVersion() { }

    public DeliveryPlanVersion(UUID deliveryPlanId, int version, UUID directiveRevisionId, String objective, String reasoning,
            String acceptanceCriteria, String suggestedSprintBoundaries, String planSnapshot,
            String createdBy, Instant createdAt) {
        this.deliveryPlanId = deliveryPlanId;
        this.version = version;
        this.directiveRevisionId = directiveRevisionId;
        this.objective = objective;
        this.reasoning = reasoning;
        this.acceptanceCriteria = acceptanceCriteria;
        this.suggestedSprintBoundaries = suggestedSprintBoundaries;
        this.planSnapshot = planSnapshot;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getDeliveryPlanId() { return deliveryPlanId; }
    public int getVersion() { return version; }
    public UUID getDirectiveRevisionId() { return directiveRevisionId; }
    public String getObjective() { return objective; }
    public String getReasoning() { return reasoning; }
    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public String getSuggestedSprintBoundaries() { return suggestedSprintBoundaries; }
    public String getPlanSnapshot() { return planSnapshot; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
