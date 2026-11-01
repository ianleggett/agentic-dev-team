package dev.yaadt.controller.delivery;

import java.util.List;
import java.util.UUID;

public record DeliveryPlanReconciliationResponse(UUID planId, int planVersion, boolean reconciliationRequired,
        List<DeliveryPlanReconciliationBead> newBeads, List<DeliveryPlanReconciliationBead> stalePlannedBeads,
        String summary) { }
