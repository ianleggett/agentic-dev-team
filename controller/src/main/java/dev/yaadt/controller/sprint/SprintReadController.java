package dev.yaadt.controller.sprint;

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
    public SprintReadController(SprintRepository sprints, SprintPlanVersionRepository plans) {
        this.sprints = sprints; this.plans = plans;
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
}
