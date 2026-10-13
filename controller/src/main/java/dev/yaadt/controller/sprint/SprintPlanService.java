package dev.yaadt.controller.sprint;

import dev.yaadt.controller.directive.DirectiveRepository;
import dev.yaadt.controller.profile.AgentProfileVersionRepository;
import dev.yaadt.controller.repository.RepositoryRepository;
import dev.yaadt.controller.user.CurrentUserService;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class SprintPlanService {
    private final SprintRepository sprints;
    private final SprintPlanVersionRepository plans;
    private final SprintStepRepository steps;
    private final DirectiveRepository directives;
    private final AgentProfileVersionRepository profiles;
    private final RepositoryRepository repositories;
    private final CurrentUserService users;
    private final Clock clock;
    public SprintPlanService(SprintRepository sprints, SprintPlanVersionRepository plans, SprintStepRepository steps,
            DirectiveRepository directives, AgentProfileVersionRepository profiles, RepositoryRepository repositories,
            CurrentUserService users, Clock clock) {
        this.sprints = sprints; this.plans = plans; this.steps = steps; this.directives = directives;
        this.profiles = profiles; this.repositories = repositories; this.users = users; this.clock = clock;
    }
    public SprintResponse create(UUID projectId, SprintPlanRequest request, String subject, String displayName) {
        validate(request);
        Instant now = Instant.now(clock);
        Sprint sprint = sprints.save(new Sprint(projectId, request.objective(), users.resolve(subject, displayName).getId(), now));
        SprintPlanVersion plan = plans.save(new SprintPlanVersion(sprint.getId(), 1, request, subject, now));
        for (SprintStepRequest step : request.steps()) steps.save(new SprintStep(plan.getId(), step, now));
        return response(sprint, plan, request.steps().size());
    }
    private void validate(SprintPlanRequest request) {
        if (!directives.existsById(request.directiveRevisionId())) invalid("Directive revision not found");
        Set<String> locks = new HashSet<>();
        for (SprintStepRequest step : request.steps()) {
            if (!profiles.existsById(step.agentProfileVersionId())) invalid("Agent profile version not found");
            if (!repositories.findAllById(step.repositoryIds()).stream().map(r -> r.getId()).collect(java.util.stream.Collectors.toSet())
                    .containsAll(step.repositoryIds())) invalid("Repository reference not found");
            if (!locks.addAll(step.resourceLocks())) invalid("Duplicate resource lock");
        }
        try {
            DependencyGraphValidator.validate(request.steps());
        } catch (IllegalArgumentException exception) {
            invalid(exception.getMessage());
        }
    }
    private SprintResponse response(Sprint sprint, SprintPlanVersion plan, int count) {
        return new SprintResponse(sprint.getId(), sprint.getProjectId(), sprint.getObjective(), sprint.getState(),
                plan.getVersion(), plan.getId(), count, sprint.getCreatedAt(), sprint.getUpdatedAt());
    }
    private void invalid(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
