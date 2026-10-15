package dev.yaadt.controller.sprint;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sprints/{sprintId}/steps")
public class SprintStepController {
    private final SprintRepository sprints;
    private final SprintPlanVersionRepository plans;
    private final SprintStepRepository steps;
    public SprintStepController(SprintRepository sprints, SprintPlanVersionRepository plans, SprintStepRepository steps) {
        this.sprints = sprints; this.plans = plans; this.steps = steps;
    }
    @GetMapping
    public List<SprintStepResponse> list(@PathVariable UUID sprintId) {
        if (!sprints.existsById(sprintId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found");
        SprintPlanVersion plan = plans.findTopBySprintIdOrderByVersionDesc(sprintId).orElse(null);
        return plan == null ? List.of() : steps.findByPlanVersionIdOrderByStepKey(plan.getId()).stream().map(SprintStepResponse::from).toList();
    }
}
