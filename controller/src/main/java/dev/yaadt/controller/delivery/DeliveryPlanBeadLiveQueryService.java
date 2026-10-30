package dev.yaadt.controller.delivery;

import dev.yaadt.controller.beads.BeadsClient;
import dev.yaadt.controller.beads.BeadsIssue;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DeliveryPlanBeadLiveQueryService {
    private final DeliveryPlanRepository plans;
    private final DeliveryPlanVersionRepository versions;
    private final DeliveryPlanBeadRepository beads;
    private final BeadsClient liveBeads;

    public DeliveryPlanBeadLiveQueryService(DeliveryPlanRepository plans, DeliveryPlanVersionRepository versions,
            DeliveryPlanBeadRepository beads, BeadsClient liveBeads) {
        this.plans = plans;
        this.versions = versions;
        this.beads = beads;
        this.liveBeads = liveBeads;
    }

    public List<DeliveryPlanBeadLiveResponse> list(UUID planId) {
        DeliveryPlan plan = plans.findById(planId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "DeliveryPlan not found"));
        DeliveryPlanVersion version = versions.findByDeliveryPlanIdAndVersion(planId, plan.getCurrentVersion())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "DeliveryPlan version not found"));
        return beads.findByDeliveryPlanVersionIdOrderByBeadId(version.getId()).stream().map(snapshot -> {
            try {
                BeadsIssue current = liveBeads.getIssue(snapshot.getBeadId());
                boolean changed = !snapshot.getTitleSnapshot().equals(current.title())
                        || !snapshot.getStatusSnapshot().equals(current.status());
                return new DeliveryPlanBeadLiveResponse(DeliveryPlanBeadResponse.from(snapshot), current.status(),
                        current.title(), true, changed);
            } catch (RuntimeException unavailable) {
                return new DeliveryPlanBeadLiveResponse(DeliveryPlanBeadResponse.from(snapshot), "UNAVAILABLE", "",
                        false, false);
            }
        }).toList();
    }
}
