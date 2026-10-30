package dev.yaadt.controller.delivery;

import dev.yaadt.controller.user.CurrentUserService;
import dev.yaadt.controller.project.ProjectRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class DeliveryPlanService {
    private final DeliveryPlanRepository plans;
    private final DeliveryPlanVersionRepository versions;
    private final DeliveryMilestoneRepository milestones;
    private final DeliveryPlanBeadRepository beads;
    private final ProjectRepository projects;
    private final CurrentUserService users;
    private final Clock clock;

    public DeliveryPlanService(DeliveryPlanRepository plans, DeliveryPlanVersionRepository versions,
            DeliveryMilestoneRepository milestones, DeliveryPlanBeadRepository beads,
            ProjectRepository projects, CurrentUserService users, Clock clock) {
        this.plans = plans;
        this.versions = versions;
        this.milestones = milestones;
        this.beads = beads;
        this.projects = projects;
        this.users = users;
        this.clock = clock;
    }

    public DeliveryPlanResponse create(UUID projectId, DeliveryPlanRequest request, String subject, String displayName) {
        requireProject(projectId);
        Instant now = Instant.now(clock);
        DeliveryPlan plan = plans.save(new DeliveryPlan(projectId, request.objective(), users.resolve(subject, displayName).getId(), now));
        saveVersion(plan, 1, request, subject, now);
        return response(plan);
    }

    @Transactional(readOnly = true)
    public List<DeliveryPlanResponse> list(UUID projectId) {
        requireProject(projectId);
        return plans.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public DeliveryPlanResponse get(UUID planId) { return response(find(planId)); }

    @Transactional(readOnly = true)
    public List<DeliveryPlanBeadResponse> beads(UUID planId) {
        DeliveryPlan plan = find(planId);
        DeliveryPlanVersion version = versions.findByDeliveryPlanIdAndVersion(planId, plan.getCurrentVersion())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DeliveryPlan version not found"));
        return beads.findByDeliveryPlanVersionIdOrderByBeadId(version.getId()).stream()
                .map(DeliveryPlanBeadResponse::from).toList();
    }

    public DeliveryPlanResponse revise(UUID planId, DeliveryPlanRequest request, String subject) {
        DeliveryPlan plan = findForUpdate(planId);
        if ("COMPLETED".equals(plan.getState()) || "SUPERSEDED".equals(plan.getState())) invalid("DeliveryPlan cannot be revised from " + plan.getState());
        Instant now = Instant.now(clock);
        int version = plan.revise(request.objective(), now);
        saveVersion(plan, version, request, subject, now);
        return response(plans.save(plan));
    }

    public DeliveryPlanResponse submit(UUID planId) {
        DeliveryPlan plan = findForUpdate(planId);
        if (!SetOf.DRAFT_OR_REJECTED.contains(plan.getState())) invalid("DeliveryPlan cannot be submitted from " + plan.getState());
        plan.submitForApproval(Instant.now(clock));
        return response(plans.save(plan));
    }

    public DeliveryPlanResponse approve(UUID planId, DeliveryPlanDecisionRequest request, String subject) {
        DeliveryPlan plan = findForUpdate(planId);
        if (!"PENDING_APPROVAL".equals(plan.getState())) invalid("DeliveryPlan is not awaiting approval");
        if (request.version() != plan.getCurrentVersion()) invalid("Only the current DeliveryPlan version can be approved");
        plan.approve(request.version(), subject, request.reason(), Instant.now(clock));
        return response(plans.save(plan));
    }

    public DeliveryPlanResponse reject(UUID planId) {
        DeliveryPlan plan = findForUpdate(planId);
        if (!"PENDING_APPROVAL".equals(plan.getState())) invalid("DeliveryPlan is not awaiting approval");
        plan.reject(Instant.now(clock));
        return response(plans.save(plan));
    }

    private void saveVersion(DeliveryPlan plan, int version, DeliveryPlanRequest request, String subject, Instant now) {
        Set<String> beadIds = new HashSet<>();
        for (DeliveryPlanBeadRequest bead : request.beads()) {
            if (!beadIds.add(bead.beadId())) invalid("Bead is listed more than once: " + bead.beadId());
        }
        DeliveryPlanVersion saved = versions.save(new DeliveryPlanVersion(plan.getId(), version, request.directiveRevisionId(), request.objective(),
                request.reasoning(), request.acceptanceCriteria(), request.suggestedSprintBoundaries(), request.planSnapshot(), subject, now));
        for (DeliveryMilestoneRequest milestone : request.milestones() == null ? List.<DeliveryMilestoneRequest>of() : request.milestones()) {
            milestones.save(new DeliveryMilestone(saved.getId(), milestone.milestoneKey(), milestone.title(), milestone.outcome(), milestone.sequenceNumber()));
        }
        for (DeliveryPlanBeadRequest bead : request.beads()) {
            beads.save(new DeliveryPlanBead(saved.getId(), bead.beadId(), bead.milestoneKey(), bead.titleSnapshot(),
                    bead.statusSnapshot(), bead.dependencyIdsSnapshot(), bead.labelsSnapshot(), bead.beadSnapshot()));
        }
    }

    private DeliveryPlanResponse response(DeliveryPlan plan) {
        DeliveryPlanVersion version = versions.findByDeliveryPlanIdAndVersion(plan.getId(), plan.getCurrentVersion()).orElse(null);
        int milestoneCount = version == null ? 0 : milestones.findByDeliveryPlanVersionIdOrderBySequenceNumber(version.getId()).size();
        int beadCount = version == null ? 0 : beads.findByDeliveryPlanVersionIdOrderByBeadId(version.getId()).size();
        return new DeliveryPlanResponse(plan.getId(), plan.getProjectId(), plan.getObjective(), plan.getState(), plan.getCurrentVersion(),
                plan.getApprovedVersion(), plan.getApprovedBy(), plan.getApprovedAt(), plan.getApprovalReason(),
                version == null ? null : version.getId(), milestoneCount, beadCount, plan.getCreatedAt(), plan.getUpdatedAt());
    }

    private DeliveryPlan find(UUID id) {
        return plans.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DeliveryPlan not found"));
    }

    private DeliveryPlan findForUpdate(UUID id) {
        return plans.findForUpdateById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DeliveryPlan not found"));
    }

    private void requireProject(UUID projectId) {
        if (!projects.existsById(projectId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
    }

    private void invalid(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }

    private static final class SetOf {
        private static final java.util.Set<String> DRAFT_OR_REJECTED = java.util.Set.of("DRAFT", "REJECTED");
    }
}
