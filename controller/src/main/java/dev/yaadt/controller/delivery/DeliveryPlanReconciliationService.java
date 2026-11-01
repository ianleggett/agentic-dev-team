package dev.yaadt.controller.delivery;

import dev.yaadt.controller.beads.BeadsClient;
import dev.yaadt.controller.beads.BeadsIssue;
import dev.yaadt.controller.beads.BeadsProjectLabels;
import dev.yaadt.controller.project.ProjectRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class DeliveryPlanReconciliationService {
    private final DeliveryPlanRepository plans;
    private final DeliveryPlanVersionRepository versions;
    private final DeliveryPlanBeadRepository planBeads;
    private final ProjectRepository projects;
    private final BeadsClient beads;

    public DeliveryPlanReconciliationService(DeliveryPlanRepository plans, DeliveryPlanVersionRepository versions,
            DeliveryPlanBeadRepository planBeads, ProjectRepository projects, BeadsClient beads) {
        this.plans = plans;
        this.versions = versions;
        this.planBeads = planBeads;
        this.projects = projects;
        this.beads = beads;
    }

    @Transactional(readOnly = true)
    public DeliveryPlanReconciliationResponse inspect(UUID planId) {
        DeliveryPlan plan = plans.findById(planId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DeliveryPlan not found"));
        return inspect(plan);
    }

    @Transactional(readOnly = true)
    public DeliveryPlanReconciliationResponse inspect(DeliveryPlan plan) {
        DeliveryPlanVersion version = versions.findByDeliveryPlanIdAndVersion(plan.getId(), plan.getCurrentVersion())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DeliveryPlan version not found"));
        String label = projects.findById(plan.getProjectId())
                .map(project -> BeadsProjectLabels.forProject(project.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        Map<String, DeliveryPlanBead> planned = new HashMap<>();
        planBeads.findByDeliveryPlanVersionIdOrderByBeadId(version.getId()).forEach(bead -> planned.put(bead.getBeadId(), bead));
        List<BeadsIssue> live = beads.listRelevantIssues(label);
        List<DeliveryPlanReconciliationBead> newBeads = live.stream()
                .filter(issue -> !planned.containsKey(issue.id()))
                .map(issue -> new DeliveryPlanReconciliationBead(issue.id(), issue.title(), issue.status(), "new live project Bead"))
                .toList();
        List<DeliveryPlanReconciliationBead> stale = planned.values().stream().filter(bead -> live.stream().noneMatch(issue -> issue.id().equals(bead.getBeadId())))
                .map(bead -> new DeliveryPlanReconciliationBead(bead.getBeadId(), bead.getTitleSnapshot(), bead.getStatusSnapshot(), "missing from the live project scope"))
                .toList();
        boolean required = !newBeads.isEmpty() || !stale.isEmpty();
        String summary = required ? "The Project Manager should reconcile new and stale Beads before the next Sprint." : "The approved plan matches the live project Bead set.";
        return new DeliveryPlanReconciliationResponse(plan.getId(), plan.getCurrentVersion(), required, newBeads, stale, summary);
    }

    public String promptContext(DeliveryPlan plan) {
        DeliveryPlanReconciliationResponse result = inspect(plan);
        return "CURRENT DELIVERY PLAN RECONCILIATION\n" + result.summary() + "\nNEW BEADS\n"
                + result.newBeads().stream().map(bead -> bead.beadId() + " status=" + bead.status() + " title=" + bead.title()).reduce("", (a, b) -> a + b + "\n")
                + "STALE PLANNED BEADS\n"
                + result.stalePlannedBeads().stream().map(bead -> bead.beadId() + " status=" + bead.status() + " title=" + bead.title()).reduce("", (a, b) -> a + b + "\n");
    }
}
