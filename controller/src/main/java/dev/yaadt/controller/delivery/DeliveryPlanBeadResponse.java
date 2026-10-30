package dev.yaadt.controller.delivery;

import java.util.UUID;

public record DeliveryPlanBeadResponse(
        UUID id,
        UUID deliveryPlanVersionId,
        String beadId,
        String milestoneKey,
        String titleSnapshot,
        String statusSnapshot,
        String dependencyIdsSnapshot,
        String labelsSnapshot,
        String beadSnapshot) {

    public static DeliveryPlanBeadResponse from(DeliveryPlanBead bead) {
        return new DeliveryPlanBeadResponse(bead.getId(), bead.getDeliveryPlanVersionId(), bead.getBeadId(),
                bead.getMilestoneKey(), bead.getTitleSnapshot(), bead.getStatusSnapshot(),
                bead.getDependencyIdsSnapshot(), bead.getLabelsSnapshot(), bead.getBeadSnapshot());
    }
}
