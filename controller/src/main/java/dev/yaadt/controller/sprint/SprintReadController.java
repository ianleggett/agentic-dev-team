package dev.yaadt.controller.sprint;

import dev.yaadt.controller.delivery.SprintBeadSnapshotRepository;
import dev.yaadt.controller.delivery.SprintBeadSnapshotResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/sprints")
public class SprintReadController {
    private final SprintRepository sprints;
    private final SprintPlanVersionRepository plans;
    private final SprintBeadSnapshotRepository beadSnapshots;
    public SprintReadController(SprintRepository sprints, SprintPlanVersionRepository plans,
            SprintBeadSnapshotRepository beadSnapshots) {
        this.sprints = sprints; this.plans = plans; this.beadSnapshots = beadSnapshots;
    }
    @GetMapping
    public List<SprintResponse> list(@PathVariable UUID projectId) {
        return sprints.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(sprint -> {
            SprintPlanVersion plan = plans.findTopBySprintIdOrderByVersionDesc(sprint.getId()).orElse(null);
            return new SprintResponse(sprint.getId(), sprint.getProjectId(), sprint.getObjective(), sprint.getState(),
                    sprint.getCurrentPlanVersion(), plan == null ? null : plan.getId(), 0,
                    sprint.getCreatedAt(), sprint.getUpdatedAt());
        }).toList();
    }

    @GetMapping("/{sprintId}/bead-snapshots")
    public List<SprintBeadSnapshotResponse> beadSnapshots(@PathVariable UUID sprintId) {
        if (!sprints.existsById(sprintId)) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Sprint not found");
        return beadSnapshots.findBySprintIdOrderByStepKeyAscBeadIdAsc(sprintId).stream()
                .map(SprintBeadSnapshotResponse::from).toList();
    }
}
