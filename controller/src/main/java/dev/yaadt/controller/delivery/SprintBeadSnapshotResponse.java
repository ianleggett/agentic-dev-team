package dev.yaadt.controller.delivery;

import java.util.UUID;

public record SprintBeadSnapshotResponse(
        UUID id,
        UUID sprintId,
        String beadId,
        String stepKey,
        String titleSnapshot,
        String statusSnapshot,
        String dependencyIdsSnapshot,
        String labelsSnapshot,
        String beadSnapshot,
        String selectionReason) {

    public static SprintBeadSnapshotResponse from(SprintBeadSnapshot snapshot) {
        return new SprintBeadSnapshotResponse(snapshot.getId(), snapshot.getSprintId(), snapshot.getBeadId(),
                snapshot.getStepKey(), snapshot.getTitleSnapshot(), snapshot.getStatusSnapshot(),
                snapshot.getDependencyIdsSnapshot(), snapshot.getLabelsSnapshot(), snapshot.getBeadSnapshot(),
                snapshot.getSelectionReason());
    }
}
