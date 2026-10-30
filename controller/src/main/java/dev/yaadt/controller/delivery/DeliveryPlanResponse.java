package dev.yaadt.controller.delivery;

import java.time.Instant;
import java.util.UUID;

public record DeliveryPlanResponse(UUID id, UUID projectId, String objective, String state,
        int currentVersion, Integer approvedVersion, String approvedBy, Instant approvedAt,
        String approvalReason, UUID versionId, int milestoneCount, int beadCount,
        Instant createdAt, Instant updatedAt) { }
