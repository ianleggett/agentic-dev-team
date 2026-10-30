package dev.yaadt.controller.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "sprint_bead_snapshots")
public class SprintBeadSnapshot {
    @Id private UUID id;
    @Column(name = "sprint_id", nullable = false) private UUID sprintId;
    @Column(name = "bead_id", nullable = false, length = 240) private String beadId;
    @Column(name = "step_key", nullable = false, length = 120) private String stepKey;
    @Column(name = "title_snapshot", nullable = false, length = 500) private String titleSnapshot;
    @Column(name = "status_snapshot", nullable = false, length = 80) private String statusSnapshot;
    @Column(name = "dependency_ids_snapshot", nullable = false, columnDefinition = "text") private String dependencyIdsSnapshot;
    @Column(name = "labels_snapshot", nullable = false, columnDefinition = "text") private String labelsSnapshot;
    @Column(name = "bead_snapshot", nullable = false, columnDefinition = "text") private String beadSnapshot;
    @Column(name = "selection_reason", nullable = false, columnDefinition = "text") private String selectionReason;

    protected SprintBeadSnapshot() { }

    public SprintBeadSnapshot(UUID sprintId, String beadId, String stepKey, String titleSnapshot,
            String statusSnapshot, String dependencyIdsSnapshot, String labelsSnapshot,
            String beadSnapshot, String selectionReason) {
        this.id = UUID.randomUUID();
        this.sprintId = sprintId;
        this.beadId = beadId;
        this.stepKey = stepKey;
        this.titleSnapshot = titleSnapshot;
        this.statusSnapshot = statusSnapshot;
        this.dependencyIdsSnapshot = dependencyIdsSnapshot;
        this.labelsSnapshot = labelsSnapshot;
        this.beadSnapshot = beadSnapshot;
        this.selectionReason = selectionReason;
    }

    public UUID getId() { return id; }
    public UUID getSprintId() { return sprintId; }
    public String getBeadId() { return beadId; }
    public String getStepKey() { return stepKey; }
    public String getTitleSnapshot() { return titleSnapshot; }
    public String getStatusSnapshot() { return statusSnapshot; }
    public String getDependencyIdsSnapshot() { return dependencyIdsSnapshot; }
    public String getLabelsSnapshot() { return labelsSnapshot; }
    public String getBeadSnapshot() { return beadSnapshot; }
    public String getSelectionReason() { return selectionReason; }
}
