package dev.yaadt.controller.delivery;

import dev.yaadt.controller.sprint.SprintPlanVersionRepository;
import dev.yaadt.controller.sprint.SprintRepository;
import dev.yaadt.controller.sprint.SprintResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DeliveryPlanSprintQueryService {
    private final DeliveryPlanRepository plans;
    private final SprintRepository sprints;
    private final SprintPlanVersionRepository sprintPlans;

    public DeliveryPlanSprintQueryService(DeliveryPlanRepository plans, SprintRepository sprints,
            SprintPlanVersionRepository sprintPlans) {
        this.plans = plans;
        this.sprints = sprints;
        this.sprintPlans = sprintPlans;
    }

    public List<SprintResponse> list(UUID planId) {
        if (!plans.existsById(planId)) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "DeliveryPlan not found");
        return sprints.findByDeliveryPlanIdOrderByCreatedAtDesc(planId).stream().map(sprint -> {
            var plan = sprintPlans.findTopBySprintIdOrderByVersionDesc(sprint.getId()).orElse(null);
            return new SprintResponse(sprint.getId(), sprint.getProjectId(), sprint.getObjective(), sprint.getState(),
                    sprint.getCurrentPlanVersion(), plan == null ? null : plan.getId(), 0,
                    sprint.getCreatedAt(), sprint.getUpdatedAt());
        }).toList();
    }
}
