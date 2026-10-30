package dev.yaadt.controller.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "delivery_milestones")
public class DeliveryMilestone {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "delivery_plan_version_id", nullable = false) private UUID deliveryPlanVersionId;
    @Column(name = "milestone_key", nullable = false, length = 120) private String milestoneKey;
    @Column(nullable = false, length = 240) private String title;
    @Column(nullable = false, columnDefinition = "text") private String outcome;
    @Column(name = "sequence_number", nullable = false) private int sequenceNumber;
    @Column(nullable = false, length = 40) private String state;

    protected DeliveryMilestone() { }

    public DeliveryMilestone(UUID deliveryPlanVersionId, String milestoneKey, String title, String outcome, int sequenceNumber) {
        this.deliveryPlanVersionId = deliveryPlanVersionId;
        this.milestoneKey = milestoneKey;
        this.title = title;
        this.outcome = outcome;
        this.sequenceNumber = sequenceNumber;
        this.state = "PLANNED";
    }

    public UUID getId() { return id; }
    public UUID getDeliveryPlanVersionId() { return deliveryPlanVersionId; }
    public String getMilestoneKey() { return milestoneKey; }
    public String getTitle() { return title; }
    public String getOutcome() { return outcome; }
    public int getSequenceNumber() { return sequenceNumber; }
    public String getState() { return state; }
}
