package dev.yaadt.controller.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import dev.yaadt.controller.sprint.SprintPlanVersionRepository;
import dev.yaadt.controller.sprint.SprintStepRepository;
import dev.yaadt.controller.profile.AgentProfileVersionRepository;
import dev.yaadt.controller.profile.AgentProfileRepository;
import dev.yaadt.controller.audit.SprintNarrativeService;
import dev.yaadt.controller.sprint.SprintRepository;
import dev.yaadt.controller.repository.RepositoryRepository;
import dev.yaadt.controller.manager.ControllerLeaseService;
import dev.yaadt.controller.beads.BeadsClient;
import dev.yaadt.controller.beads.BeadsIssue;
import dev.yaadt.controller.beads.BeadsProjectLabels;
import dev.yaadt.controller.project.ProjectRepository;

@Service
@Transactional
public class AgentRunService {
    private final AgentRunRepository runs;
    private final RunEventRepository events;
    private final AgentExecutor executor;
    private final SprintPlanVersionRepository plans;
    private final SprintStepRepository steps;
    private final AgentProfileVersionRepository profiles;
    private final AgentProfileRepository profileDefinitions;
    private final SprintRepository sprints;
    private final SprintNarrativeService narrative;
    private final RepositoryRepository repositories;
    private final Clock clock;
    private final ObjectMapper mapper;
    private final ControllerLeaseService leases;
    private final ProjectRepository projects;
    private final BeadsClient beads;

    public AgentRunService(AgentRunRepository runs, RunEventRepository events, AgentExecutor executor,
            SprintPlanVersionRepository plans, SprintStepRepository steps, AgentProfileVersionRepository profiles,
            AgentProfileRepository profileDefinitions, SprintRepository sprints, SprintNarrativeService narrative, RepositoryRepository repositories, Clock clock,
            ControllerLeaseService leases, ProjectRepository projects, BeadsClient beads) {
        this.runs = runs; this.events = events; this.executor = executor; this.plans = plans; this.steps = steps;
        this.profiles = profiles; this.sprints = sprints; this.narrative = narrative; this.repositories = repositories; this.clock = clock;
        this.profileDefinitions = profileDefinitions; this.mapper = new ObjectMapper(); this.leases = leases;
        this.projects = projects; this.beads = beads;
    }

    public AgentRunResponse execute(UUID stepId) {
        AgentRunResponse run = prepareRun(stepId);
        return executeRun(run.id());
    }

    public AgentRunResponse createRun(UUID stepId) {
        var latest = runs.findByStepIdOrderByAttemptNumberDesc(stepId).stream().findFirst().orElse(null);
        if (latest != null && !latest.getState().equals("FAILED") && !latest.getState().equals("TIMED_OUT")) {
            return AgentRunResponse.from(latest);
        }
        int attempt = latest == null ? 1 : latest.getAttemptNumber() + 1;
        String key = "step:" + stepId + ":attempt:" + attempt;
        AgentRun run = runs.findByIdempotencyKey(key).orElseGet(() ->
                runs.save(new AgentRun(stepId, attempt, key, "/workspace/runs/" + stepId, Instant.now(clock))));
        return AgentRunResponse.from(run);
    }

    public AgentRunResponse prepareRun(UUID stepId) {
        AgentRunResponse response = createRun(stepId);
        if (!"CREATED".equals(response.state())) return response;
        var step = steps.findById(stepId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint step not found"));
        var profile = profileFor(step, response.attemptNumber());
        String prompt = promptFor(step, profile);
        String workspace = workspaceFor(step, "/workspace/runs/" + stepId);
        String metadata = metadata(step, profile, workspace, response.attemptNumber());
        if (events.findByAgentRunIdOrderBySequenceNumberAsc(response.id()).isEmpty()) {
            events.saveAll(List.of(
                    new RunEvent(response.id(), new AgentRunEvent(1, "RUN_METADATA", metadata, false, Instant.now(clock))),
                    new RunEvent(response.id(), new AgentRunEvent(2, "INPUT", prompt, false, Instant.now(clock)))));
        }
        return response;
    }

    public AgentRunResponse executeRun(UUID runId) {
        AgentRun run = runs.findById(runId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent run not found"));
        if (run.getState().equals("SUCCEEDED")) return AgentRunResponse.from(run);
        run.start(Instant.now(clock));
        var step = steps.findById(run.getStepId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint step not found"));
        var profile = profileFor(step, run.getAttemptNumber());
        var backend = profile == null ? null : new AgentBackendConfig(profile.getProviderIdentifier(), profile.getModelIdentifier(),
                profile.getBaseUrl(), profile.getApiKey());
        String prompt = promptFor(step, profile);
        String workspace = workspaceFor(step, run.getWorkspaceId());
        List<dev.yaadt.controller.repository.Repository> stepRepositories = repositoryList(step);
        String buildCommands = stepRepositories.stream().map(dev.yaadt.controller.repository.Repository::getBuildCommands)
                .filter(value -> value != null && !value.isBlank()).reduce((left, right) -> left + "\n" + right).orElse("");
        String testCommands = stepRepositories.stream().map(dev.yaadt.controller.repository.Repository::getTestCommands)
                .filter(value -> value != null && !value.isBlank()).reduce((left, right) -> left + "\n" + right).orElse("");
        List<String> beadIds = step.getBeadIds().isBlank() ? List.of() : Arrays.stream(step.getBeadIds().split(",")).map(String::trim).filter(value -> !value.isBlank()).toList();
        AgentRunResult result = executor.execute(new AgentRunRequest(run.getIdempotencyKey(), run.getStepId(),
                run.getAttemptNumber(), workspace, prompt, java.util.Map.of(), backend, beadIds, buildCommands, testCommands));
        List<RunEvent> existing = events.findByAgentRunIdOrderBySequenceNumberAsc(run.getId());
        List<AgentRunEvent> transcript = new ArrayList<>();
        for (int index = 0; index < result.events().size(); index++) {
            AgentRunEvent event = result.events().get(index);
            transcript.add(new AgentRunEvent(existing.size() + index + 1, event.eventType(), event.payload(), event.redacted(), event.createdAt()));
        }
        events.saveAll(transcript.stream().map(event -> new RunEvent(run.getId(), event)).toList());
        run.finish(result, Instant.now(clock));
        return AgentRunResponse.from(runs.save(run));
    }

    public String executeSprint(UUID sprintId) {
        var plan = plans.findTopBySprintIdOrderByVersionDesc(sprintId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint plan not found"));
        event(sprintId, "execution-started", "RUNNING", "workflow activity started");
        int completed = 0;
        String sprintOwner = "sprint:" + sprintId;
        leases.acquire("execution:global", sprintOwner);
        try {
            List<dev.yaadt.controller.sprint.SprintStep> pending = new ArrayList<>(steps.findByPlanVersionIdOrderByStepKey(plan.getId()));
            Set<String> sprintBeadIds = new LinkedHashSet<>();
            pending.forEach(step -> sprintBeadIds.addAll(beadIds(step)));
            List<String> completedKeys = new ArrayList<>();
            while (!pending.isEmpty()) {
                var step = pending.stream().filter(candidate -> dependenciesComplete(candidate, completedKeys)).findFirst().orElseThrow(() ->
                        new IllegalStateException("Sprint step dependencies cannot be scheduled"));
                pending.remove(step);
                String owner = sprintOwner + ":step:" + step.getId();
                List<String> lockKeys = lockKeys(step);
                for (String lockKey : lockKeys) leases.acquire("resource:" + lockKey, owner);
                try {
                var profile = profiles.findById(step.getAgentProfileVersionId()).orElse(null);
                String agent = "step=" + step.getStepKey() + " agent=" + step.getAgentRole();
                if (profile != null) agent += " profile=" + profile.getPromptReference() + " model=" + profile.getProviderIdentifier() + "/" + profile.getModelIdentifier();
                event(sprintId, "step-started", "RUNNING", agent);
                AgentRunResponse run = execute(step.getId());
                String detail = "step=" + step.getStepKey() + " run=" + run.id();
                if (run.errorCode() != null) detail += " errorCode=" + run.errorCode();
                if (run.errorMessage() != null) detail += " error=" + run.errorMessage();
                event(sprintId, "step-completed", run.state(), detail);
                if (!"SUCCEEDED".equals(run.state())) throw new IllegalStateException("Required step failed: " + step.getStepKey());
                completed++;
                completedKeys.add(step.getStepKey());
                } finally {
                    for (int index = lockKeys.size() - 1; index >= 0; index--) leases.release("resource:" + lockKeys.get(index), owner);
                }
            }
            completeBeads(sprintId, sprintBeadIds);
            sprints.findById(sprintId).ifPresent(sprint -> sprint.transition("COMPLETED", Instant.now(clock)));
            event(sprintId, "execution-completed", "COMPLETED", "steps=" + completed);
            return "EXECUTED_STEPS=" + completed;
        } catch (RuntimeException failure) {
            sprints.findById(sprintId).ifPresent(sprint -> sprint.transition("FAILED", Instant.now(clock)));
            event(sprintId, "execution-failed", "FAILED", failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage());
            throw failure;
        } finally {
            leases.release("execution:global", sprintOwner);
        }
    }

    private boolean dependenciesComplete(dev.yaadt.controller.sprint.SprintStep step, List<String> completedKeys) {
        String dependencies = step.getDependencyKeys();
        return dependencies == null || dependencies.isBlank()
                || Arrays.stream(dependencies.split(",")).map(String::trim).filter(value -> !value.isBlank()).allMatch(completedKeys::contains);
    }

    private List<String> lockKeys(dev.yaadt.controller.sprint.SprintStep step) {
        String locks = step.getResourceLocks();
        if (locks == null || locks.isBlank()) return List.of();
        return Arrays.stream(locks.split(",")).map(String::trim).filter(value -> !value.isBlank()).sorted().toList();
    }

    private List<String> beadIds(dev.yaadt.controller.sprint.SprintStep step) {
        if (step.getBeadIds() == null || step.getBeadIds().isBlank()) return List.of();
        return Arrays.stream(step.getBeadIds().split(",")).map(String::trim).filter(value -> !value.isBlank()).toList();
    }

    private void completeBeads(UUID sprintId, Set<String> beadIds) {
        for (String beadId : beadIds) {
            BeadsIssue issue = beads.getIssue(beadId);
            if (Set.of("closed", "completed", "cancelled", "canceled", "done").contains(issue.status().toLowerCase())) {
                event(sprintId, "bead-completion-skipped", "COMPLETED", "bead=" + beadId + " status=" + issue.status());
                continue;
            }
            beads.closeIssue(beadId, "Sprint completed successfully: " + sprintId);
            event(sprintId, "bead-completed", "COMPLETED", "bead=" + beadId);
        }
    }

    private void event(UUID sprintId, String type, String state, String payload) {
        narrative.record(sprintId, type, state, payload);
    }

    private String promptFor(dev.yaadt.controller.sprint.SprintStep step,
            dev.yaadt.controller.profile.AgentProfileVersion profile) {
        return (profile == null ? "" : profile.getTeamPrompt() + "\n\n")
                + "Execute step " + step.getStepKey() + ": " + step.getSuccessCriteria()
                + liveBeadsContext(step);
    }

    private String liveBeadsContext(dev.yaadt.controller.sprint.SprintStep step) {
        if (step.getBeadIds().isBlank()) return "";
        var plan = plans.findById(step.getPlanVersionId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint plan not found"));
        var sprint = sprints.findById(plan.getSprintId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found"));
        var project = projects.findById(sprint.getProjectId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        String label = BeadsProjectLabels.forProject(project.getBeadsProject());
        StringBuilder context = new StringBuilder("\n\nLIVE BEADS CONTEXT (fetched immediately before execution)\n");
        for (String beadId : Arrays.stream(step.getBeadIds().split(",")).map(String::trim).filter(value -> !value.isBlank()).toList()) {
            BeadsIssue issue = beads.getIssue(beadId);
            if (!issue.labels().contains(label)) throw new IllegalStateException("Bead is outside project scope: " + beadId);
            if (Set.of("closed", "completed", "cancelled", "canceled", "done").contains(issue.status().toLowerCase())) {
                throw new IllegalStateException("Bead is terminal: " + beadId);
            }
            context.append("id=").append(issue.id()).append(" status=").append(issue.status())
                    .append(" labels=").append(issue.labels()).append(" dependsOn=").append(issue.dependencyIds())
                    .append(" title=").append(issue.title())
                    .append(" description=").append(issue.description()).append("\n");
        }
        return context.toString();
    }

    private dev.yaadt.controller.profile.AgentProfileVersion profileFor(
            dev.yaadt.controller.sprint.SprintStep step, int attempt) {
        if (attempt > 1) {
            return profileDefinitions.findByRole(step.getAgentRole())
                    .flatMap(profile -> profiles.findTopByProfileIdOrderByVersionDesc(profile.getId()))
                    .orElse(null);
        }
        return profiles.findById(step.getAgentProfileVersionId()).orElse(null);
    }

    private String metadata(dev.yaadt.controller.sprint.SprintStep step,
            dev.yaadt.controller.profile.AgentProfileVersion profile, String workspace, int attempt) {
        try {
            return mapper.writeValueAsString(java.util.Map.of(
                    "stepId", step.getId(),
                    "agentProfileVersionId", step.getAgentProfileVersionId(),
                    "agentRole", step.getAgentRole(),
                    "workspace", workspace,
                    "provider", profile == null ? "unknown" : profile.getProviderIdentifier(),
                    "model", profile == null ? "unknown" : profile.getModelIdentifier(),
                    "llmUrl", profile == null ? "unknown" : profile.getBaseUrl(),
                    "profileVersion", profile == null ? "unknown" : profile.getVersion(),
                    "attempt", attempt));
        } catch (Exception exception) {
            return "{}";
        }
    }

    private String workspaceFor(dev.yaadt.controller.sprint.SprintStep step, String fallback) {
        String ids = step.getRepositoryIds();
        int start = ids.indexOf('[');
        int end = ids.indexOf(']');
        if (start < 0 || end <= start) return fallback;
        String first = ids.substring(start + 1, end).split(",")[0].trim();
        try {
            return repositories.findById(UUID.fromString(first))
                    .map(repository -> "/workspace/" + repository.effectiveWorkspacePath())
                    .orElse(fallback);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private List<dev.yaadt.controller.repository.Repository> repositoryList(dev.yaadt.controller.sprint.SprintStep step) {
        String ids = step.getRepositoryIds();
        int start = ids.indexOf('['), end = ids.indexOf(']');
        if (start < 0 || end <= start) return List.of();
        return Arrays.stream(ids.substring(start + 1, end).split(",")).map(String::trim).filter(value -> !value.isBlank()).flatMap(value -> {
            try { return repositories.findById(UUID.fromString(value)).stream(); }
            catch (IllegalArgumentException ignored) { return java.util.stream.Stream.empty(); }
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<RunEventResponse> events(UUID runId) {
        if (!runs.existsById(runId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent run not found");
        return events.findByAgentRunIdOrderBySequenceNumberAsc(runId).stream().map(RunEventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AgentRunResponse get(UUID runId) {
        return runs.findById(runId).map(AgentRunResponse::from).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent run not found"));
    }

    @Transactional(readOnly = true)
    public List<AgentRunResponse> list(UUID stepId) {
        return runs.findByStepIdOrderByAttemptNumberDesc(stepId).stream().map(AgentRunResponse::from).toList();
    }
}
