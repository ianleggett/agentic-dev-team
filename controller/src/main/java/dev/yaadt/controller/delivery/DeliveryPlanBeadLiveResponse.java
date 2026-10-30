package dev.yaadt.controller.delivery;

public record DeliveryPlanBeadLiveResponse(
        DeliveryPlanBeadResponse snapshot,
        String liveStatus,
        String liveTitle,
        boolean present,
        boolean changed) {
}
