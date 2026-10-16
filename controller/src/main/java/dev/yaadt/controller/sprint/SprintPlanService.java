package dev.yaadt.controller.sprint;

import dev.yaadt.controller.directive.DirectiveRepository;
import dev.yaadt.controller.profile.AgentProfileVersionRepository;
import dev.yaadt.controller.profile.AgentProfileRepository;
import dev.yaadt.controller.repository.RepositoryRepository;
import dev.yaadt.controller.user.CurrentUserService;
import dev.yaadt.controller.audit.AuditEvent;
import dev.yaadt.controller.audit.AuditEventRepository;
import dev.yaadt.controller.beads.BeadsClient;
import dev.yaadt.controller.beads.BeadsIssue;
import dev.yaadt.controller.beads.BeadsProjectLabels;
import dev.yaadt.controller.project.ProjectRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final AgentProfileRepository profileDefinitions;
    private final RepositoryRepository repositories;
    private final CurrentUserService users;
    private final Clock clock;
    private final AuditEventRepository audit;
    private final SprintCommandIdempotencyRepository idempotency;
    private final SprintCommandRepository commands;
    private final SprintCommandOutboxRepository outbox;
    private final ObjectMapper mapper;
    private final ProjectRepository projects;
    private final BeadsClient beads;
    public SprintPlanService(SprintRepository sprints, SprintPlanVersionRepository plans, SprintStepRepository steps,
            DirectiveRepository directives, AgentProfileVersionRepository profiles, RepositoryRepository repositories,
            AgentProfileRepository profileDefinitions, CurrentUserService users, Clock clock, AuditEventRepository audit,
            SprintCommandIdempotencyRepository idempotency, SprintCommandRepository commands, SprintCommandOutboxRepository outbox,
            ObjectMapper mapper, ProjectRepository projects, BeadsClient beads) {
        this.sprints = sprints; this.plans = plans; this.steps = steps; this.directives = directives;
        this.profiles = profiles; this.profileDefinitions = profileDefinitions; this.repositories = repositories; this.users = users; this.clock = clock;
        this.audit = audit; this.idempotency = idempotency; this.projects = projects; this.beads = beads;
        this.commands = commands; this.outbox = outbox; this.mapper = mapper;
    }
    public SprintResponse create(UUID projectId, SprintPlanRequest request, String subject, String displayName) {
        validate(projectId, request);
        Instant now = Instant.now(clock);
        Sprint sprint = sprints.save(new Sprint(projectId, request.objective(), users.resolve(subject, displayName).getId(), now));
        SprintPlanVersion plan = plans.save(new SprintPlanVersion(sprint.getId(), 1, request, subject, now));
        for (SprintStepRequest step : request.steps()) steps.save(new SprintStep(plan.getId(), step, now));
        return response(sprint, plan, request.steps().size());
    }
    public SprintResponse createManual(UUID projectId, ManualSprintRequest request, String subject, String displayName) {
        if (!directives.existsById(request.directiveRevisionId())) invalid("Directive revision not found");
        if (repositories.findAllById(request.repositoryIds()).size() != request.repositoryIds().size()) invalid("Repository reference not found");
        Instant now = Instant.now(clock);
        Sprint sprint = sprints.save(new Sprint(projectId, request.objective(), users.resolve(subject, displayName).getId(), now));
        SprintPlanVersion plan = plans.save(new SprintPlanVersion(sprint.getId(), 1,
                new SprintPlanRequest(request.objective(), request.directiveRevisionId(), "Manual ordered agent rotation",
                        "Human approval required before execution", request.failurePolicy(), List.of()), subject, now));
        for (int index = 0; index < request.agentProfileVersionIds().size(); index++) {
            UUID profileVersionId = request.agentProfileVersionIds().get(index);
            var profile = profiles.findById(profileVersionId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.CONFLICT, "Agent profile version not found"));
            String role = profileDefinitions.findById(profile.getProfileId()).orElseThrow().getRole();
            String key = String.format("%03d-%s", index + 1, role);
            List<String> depends = index == 0 ? List.of() : List.of(String.format("%03d-%s", index,
                    profileDefinitions.findById(profiles.findById(request.agentProfileVersionIds().get(index - 1)).orElseThrow().getProfileId()).orElseThrow().getRole()));
            steps.save(new SprintStep(plan.getId(), new SprintStepRequest(key, List.of(), role, profileVersionId,
                    request.repositoryIds(), request.objective(), request.failurePolicy(), depends, List.of()), now));
        }
        return response(sprint, plan, request.agentProfileVersionIds().size());
    }
    public SprintResponse replan(UUID sprintId, SprintPlanRequest request, String subject, String displayName, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var prior = idempotency.findByKey(idempotencyKey).orElse(null);
            if (prior != null) {
                if (!prior.getSprintId().equals(sprintId) || !prior.getAction().equals("replan")
                        || !prior.getReason().equals(request.objective())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key was already used for another command");
                }
                Sprint sprint = sprints.findById(sprintId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found"));
                return response(sprint, plans.findTopBySprintIdOrderByVersionDesc(sprintId).orElseThrow(), 0);
            }
        }
        Sprint sprint = sprints.findById(sprintId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found"));
        validate(sprint.getProjectId(), request);
        if (sprint.getState().equals("RUNNING") || sprint.getState().equals("COMPLETED")
                || sprint.getState().equals("CANCELLED")) {
            invalid("Sprint cannot be replanned from " + sprint.getState());
        }
        Instant now = Instant.now(clock);
        int version = sprint.getCurrentPlanVersion() + 1;
        sprint.replan(version, request.objective(), now);
        SprintPlanVersion plan = plans.save(new SprintPlanVersion(sprintId, version, request, subject, now));
        for (SprintStepRequest step : request.steps()) steps.save(new SprintStep(plan.getId(), step, now));
        String actor = users.resolve(subject, displayName).getSubject();
        audit.save(new AuditEvent(actor, sprintId, "replan", "DRAFT", "DRAFT", request.objective(), now));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotency.save(new SprintCommandIdempotency(idempotencyKey, sprintId, "replan", version, request.objective(), "DRAFT", now));
        }
        return response(sprints.save(sprint), plan, request.steps().size());
    }
    public SprintCommandResponse submitReplan(UUID sprintId, SprintPlanRequest request, String subject, String displayName, String key) {
        if (key == null || key.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key is required");
        Sprint sprint = sprints.findById(sprintId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found"));
        var prior = commands.findBySprintIdAndIdempotencyKey(sprintId, key).orElse(null);
        if (prior != null) {
            if (!prior.getAction().equals("replan") || !prior.getReason().equals(request.objective()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key was already used for another command");
            return new SprintCommandResponse(prior.getId(), sprintId, "replan", prior.getStatus(), prior.getResultState());
        }
        try {
            String payload = mapper.writeValueAsString(request);
            Instant now = Instant.now(clock);
            SprintCommand command = commands.save(new SprintCommand(sprintId, key, "replan", sprint.getCurrentPlanVersion(), request.objective(), payload, subject, displayName, now));
            outbox.save(new SprintCommandOutbox(command.getId(), now));
            return new SprintCommandResponse(command.getId(), sprintId, "replan", command.getStatus(), command.getResultState());
        } catch (Exception failure) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid replan request", failure);
        }
    }
    public SprintResponse applyReplan(SprintCommand command) {
        try {
            SprintPlanRequest request = mapper.readValue(command.getPayload(), SprintPlanRequest.class);
            return replan(command.getSprintId(), request, command.getActorSubject(), command.getActorName(), null);
        } catch (Exception failure) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid replan payload", failure);
        }
    }
    private void validate(UUID projectId, SprintPlanRequest request) {
        if (!directives.existsById(request.directiveRevisionId())) invalid("Directive revision not found");
        for (SprintStepRequest step : request.steps()) {
            if (!profiles.existsById(step.agentProfileVersionId())) invalid("Agent profile version not found");
            if (!repositories.findAllById(step.repositoryIds()).stream().map(r -> r.getId()).collect(java.util.stream.Collectors.toSet())
                    .containsAll(step.repositoryIds())) invalid("Repository reference not found");
            if (new HashSet<>(step.resourceLocks()).size() != step.resourceLocks().size()) {
                invalid("Duplicate resource lock in step: " + step.stepKey());
            }
        }
        try {
            DependencyGraphValidator.validate(request.steps());
        } catch (IllegalArgumentException exception) {
            invalid(exception.getMessage());
        }
        var project = projects.findById(projectId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        Map<String, BeadsIssue> relevant = beads.listRelevantIssues(BeadsProjectLabels.forProject(project.getBeadsProject())).stream()
                .collect(java.util.stream.Collectors.toMap(BeadsIssue::id, issue -> issue, (left, right) -> left));
        for (SprintStepRequest step : request.steps()) {
            for (String beadId : step.beadIds()) {
                BeadsIssue issue = relevant.get(beadId);
                if (issue == null) invalid("Bead is not in the project scope: " + beadId);
                if (Set.of("closed", "completed", "cancelled", "canceled", "done").contains(issue.status().toLowerCase())) {
                    invalid("Bead is terminal: " + beadId);
                }
            }
        }
    }
    private SprintResponse response(Sprint sprint, SprintPlanVersion plan, int count) {
        return new SprintResponse(sprint.getId(), sprint.getProjectId(), sprint.getObjective(), sprint.getState(),
                plan.getVersion(), plan.getId(), count, sprint.getCreatedAt(), sprint.getUpdatedAt());
    }
    private void invalid(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
