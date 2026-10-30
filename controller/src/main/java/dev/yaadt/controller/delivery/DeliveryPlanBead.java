package dev.yaadt.controller.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "delivery_plan_beads")
public class DeliveryPlanBead {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "delivery_plan_version_id", nullable = false) private UUID deliveryPlanVersionId;
    @Column(name = "bead_id", nullable = false, length = 240) private String beadId;
    @Column(name = "milestone_key", length = 120) private String milestoneKey;
    @Column(name = "title_snapshot", nullable = false, length = 500) private String titleSnapshot;
    @Column(name = "status_snapshot", nullable = false, length = 80) private String statusSnapshot;
    @Column(name = "dependency_ids_snapshot", nullable = false, columnDefinition = "text") private String dependencyIdsSnapshot;
    @Column(name = "labels_snapshot", nullable = false, columnDefinition = "text") private String labelsSnapshot;
    @Column(name = "bead_snapshot", nullable = false, columnDefinition = "text") private String beadSnapshot;

    protected DeliveryPlanBead() { }

    public DeliveryPlanBead(UUID deliveryPlanVersionId, String beadId, String milestoneKey, String titleSnapshot,
            String statusSnapshot, String dependencyIdsSnapshot, String labelsSnapshot, String beadSnapshot) {
        this.deliveryPlanVersionId = deliveryPlanVersionId;
        this.beadId = beadId;
        this.milestoneKey = milestoneKey;
        this.titleSnapshot = titleSnapshot;
        this.statusSnapshot = statusSnapshot;
        this.dependencyIdsSnapshot = dependencyIdsSnapshot;
        this.labelsSnapshot = labelsSnapshot;
        this.beadSnapshot = beadSnapshot;
    }

    public UUID getId() { return id; }
    public UUID getDeliveryPlanVersionId() { return deliveryPlanVersionId; }
    public String getBeadId() { return beadId; }
    public String getMilestoneKey() { return milestoneKey; }
    public String getTitleSnapshot() { return titleSnapshot; }
    public String getStatusSnapshot() { return statusSnapshot; }
    public String getDependencyIdsSnapshot() { return dependencyIdsSnapshot; }
    public String getLabelsSnapshot() { return labelsSnapshot; }
    public String getBeadSnapshot() { return beadSnapshot; }
}
