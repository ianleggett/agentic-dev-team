package dev.yaadt.controller.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import dev.yaadt.controller.audit.SprintNarrativeService;
import dev.yaadt.controller.beads.BeadsClient;
import dev.yaadt.controller.beads.BeadsIssue;
import dev.yaadt.controller.beads.BeadsProjectLabels;
import dev.yaadt.controller.directive.DirectiveRepository;
import dev.yaadt.controller.directive.DirectiveRevision;
import dev.yaadt.controller.user.CurrentUserService;
import dev.yaadt.controller.executor.AgentExecutor;
import dev.yaadt.controller.executor.AgentBackendConfig;
import dev.yaadt.controller.executor.AgentRunRequest;
import dev.yaadt.controller.executor.AgentRunResult;
import dev.yaadt.controller.profile.AgentProfileRepository;
import dev.yaadt.controller.profile.AgentProfileVersionRepository;
import dev.yaadt.controller.project.ProjectRepository;
import dev.yaadt.controller.repository.RepositoryRepository;
import dev.yaadt.controller.sprint.SprintStepRequest;
import dev.yaadt.controller.delivery.DeliveryMilestoneRequest;
import dev.yaadt.controller.delivery.DeliveryPlanBeadRequest;
import dev.yaadt.controller.delivery.DeliveryPlanRequest;
import dev.yaadt.controller.delivery.DeliveryPlanService;
import dev.yaadt.controller.delivery.DeliveryPlanRepository;
import dev.yaadt.controller.delivery.DeliveryPlanReconciliationService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class ProjectManagerPlanningService {
    private final ProjectRepository projects;
    private final RepositoryRepository repositories;
    private final DirectiveRepository directives;
    private final AgentProfileRepository profiles;
    private final AgentProfileVersionRepository versions;
    private final ProjectManagerRepository assignments;
    private final AgentExecutor executor;
    private final ObjectMapper mapper;
    private final SprintNarrativeService narrative;
    private final Clock clock;
    private final BeadsClient beads;
    private final ProjectManagerPlanningAttemptService attempts;
    private final CurrentUserService users;
    private final DeliveryPlanService deliveryPlans;
    private final DeliveryPlanRepository planRepository;
    private final DeliveryPlanReconciliationService reconciliation;

    public ProjectManagerPlanningService(ProjectRepository projects, RepositoryRepository repositories,
            DirectiveRepository directives, AgentProfileRepository profiles, AgentProfileVersionRepository versions,
            ProjectManagerRepository assignments, AgentExecutor executor, ObjectMapper mapper,
            SprintNarrativeService narrative, Clock clock, BeadsClient beads, ProjectManagerPlanningAttemptService attempts,
            CurrentUserService users, DeliveryPlanService deliveryPlans, DeliveryPlanRepository planRepository,
            DeliveryPlanReconciliationService reconciliation) {
        this.projects = projects; this.repositories = repositories; this.directives = directives; this.profiles = profiles;
        this.assignments = assignments;
        this.versions = versions; this.executor = executor; this.mapper = mapper;
        this.narrative = narrative; this.clock = clock; this.beads = beads; this.attempts = attempts;
        this.users = users;
        this.deliveryPlans = deliveryPlans;
        this.planRepository = planRepository;
        this.reconciliation = reconciliation;
    }

    @Transactional
    public UUID generate(UUID projectId) {
        var project = projects.findById(projectId).orElseThrow(() -> new IllegalArgumentException("Project not found"));
        var directive = directives.findTopByProjectIdOrderByVersionDesc(projectId).orElseGet(() -> {
            if (project.getObjective() == null || project.getObjective().isBlank()) {
                throw new IllegalArgumentException("Project has no directive");
            }
            int version = directives.findTopByProjectIdOrderByVersionDesc(projectId)
                    .map(existing -> existing.getVersion() + 1).orElse(1);
            return directives.save(new DirectiveRevision(projectId, version, project.getObjective(),
                    users.resolve(null, "Project directive").getId(), clock.instant()));
        });
        var managerVersion = assignments.findByProjectId(projectId)
                .map(a -> versions.findById(a.getAgentProfileVersionId()).orElse(null)).orElse(null);
        if (managerVersion == null) {
            var manager = profiles.findByRole("project-manager").orElseThrow(() -> new IllegalArgumentException("Project manager profile not found"));
            managerVersion = versions.findTopByProfileIdOrderByVersionDesc(manager.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Project manager profile version not found"));
        }
        String projectLabel = BeadsProjectLabels.forProject(project.getName());
        List<BeadsIssue> relevantBeads = beads.listRelevantIssues(projectLabel);
        String beadsContext = relevantBeads.stream()
                .map(issue -> "id=" + issue.id() + " status=" + issue.status() + " labels=" + issue.labels()
                        + " dependsOn=" + issue.dependencyIds() + " title=" + issue.title() + " description=" + issue.description())
                .reduce("", (a, b) -> a + b + "\n");
        var currentPlan = planRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .filter(candidate -> "APPROVED".equals(candidate.getState())).findFirst().orElse(null);
        String sprintObjective = resolveSprintObjective(assignments.findByProjectId(projectId).map(ProjectManager::getPlanningObjective).orElse(""),
                currentPlan == null ? "" : currentPlan.getObjective(), directive.getContent(), project.getObjective());
        String context = "PROJECT\nname=" + project.getName() + " sprintObjective=" + sprintObjective + " beadsProject=" + project.getBeadsProject()
                + " beadsProjectLabel=" + projectLabel
                + " defaultBranch=" + project.getDefaultBranch() + "\nDIRECTIVE\n" + directive.getContent()
                + "\nREPOSITORIES\n" + repositories.findByProjectIdOrderByName(projectId).stream()
                .map(r -> r.getId() + " name=" + r.getName() + " role=" + r.getRole() + " path=" + r.getRelativeWorkspacePath()
                        + " build=" + r.getBuildCommands() + " test=" + r.getTestCommands())
                 .reduce("", (a, b) -> a + b + "\n")
                 + "\nRELEVANT BEADS\n" + beadsContext;
        boolean reconciliationMode = currentPlan != null;
        if (reconciliationMode) context += "\n" + reconciliation.promptContext(currentPlan);
        String catalog = "AGENT CATALOG\n"
                + "spec=turn requirements into specifications; do not implement code\n"
                + "architect=design system boundaries and technical approach\n"
                + "tech-lead=coordinate implementation strategy and integration\n"
                + "fullstack=implement application code and fix build failures\n"
                + "database=implement schema, queries, and migrations\n"
                + "devops=implement CI/CD, infrastructure, and deployment\n"
                + "qa=run tests, verify compilation, and report regressions\n"
                + "review=review completed code for correctness and maintainability\n"
                + "security=review security and permissions\n"
                + "docs=write and update documentation\n"
                + "ui-assessor=assess user interface behavior and accessibility\n";
        String modeInstruction = reconciliationMode
                ? "This is a reconciliation phase for an existing approved DeliveryPlan. Treat CURRENT DELIVERY PLAN RECONCILIATION as authoritative input, preserve still-valid planned work, add applicable new project Beads, remove or defer stale/out-of-scope work, and produce a revised plan version. Do not silently ignore a ready new Bead."
                : "Create the initial DeliveryPlan.";
        String prompt = managerVersion.getTeamPrompt() + "\n\n" + modeInstruction + " Create a durable delivery plan for the explicit SPRINT OBJECTIVE in the PROJECT context. Preserve that objective as the delivery-plan objective unless a more precise wording is required. The plan will be approved once, then a scheduler will generate bounded sprints just in time. You MUST return a non-empty objective, at least one milestone, at least one step, and at least one Bead assignment. If RELEVANT BEADS is empty or does not cover the work, you MUST create at least one proposedBead and reference its proposalKey from a step's beadIds. Use only beadIds from RELEVANT BEADS, or proposalKey values from proposedBeads for current gaps. Return JSON only with fields objective, reasoning, approvalPolicy, failurePolicy, suggestedSprintBoundaries, milestones, proposedBeads, and steps. Each milestone must contain milestoneKey, title, and outcome. proposedBeads must contain proposalKey, title, description, and labels. Each step must contain stepKey, beadIds, agentRole, repositoryIds, successCriteria, failurePolicy, dependsOn, and resourceLocks. Use only roles from the catalog. Assign implementation/build/compile-fixing work to fullstack (or database/devops when the repository task is specifically those domains), and assign verification/testing work to qa. Never assign spec to implementation or compilation work.\n\n" + catalog + "\n" + context;
        UUID attemptId = attempts.start(projectId, prompt, managerVersion.getProviderIdentifier(), managerVersion.getModelIdentifier(), managerVersion.getBaseUrl());
        AgentRunResult result;
        try {
            result = executor.execute(new AgentRunRequest("project-manager:" + projectId + ":" + clock.instant(), UUID.randomUUID(), 1,
                    "/workspace/runs/project-manager-" + projectId, prompt, java.util.Map.of(),
                    new AgentBackendConfig(managerVersion.getProviderIdentifier(), managerVersion.getModelIdentifier(), managerVersion.getBaseUrl(), managerVersion.getApiKey())));
        } catch (RuntimeException failure) {
            attempts.finish(attemptId, "FAILED", "", "EXECUTOR_FAILED", failure.getMessage());
            throw failure;
        }
        String modelOutput = result.events().stream().map(event -> event.payload()).reduce("", (a, b) -> a + b);
        if (result.state() != AgentRunResult.State.SUCCEEDED) {
            attempts.finish(attemptId, result.state().name(), modelOutput, result.errorCode(), result.errorMessage());
            throw new IllegalStateException(result.errorCode() + ": " + result.errorMessage());
        }
        try {
            JsonNode plan = mapper.readTree(extractJson(result));
            validateGeneratedPlan(plan);
            Map<String, String> beadIds = createProposedBeads(plan, projectLabel, relevantBeads);
            List<SprintStepRequest> stepRequests = new ArrayList<>();
            for (JsonNode step : plan.withArray("steps")) {
                String requestedRole = step.path("agentRole").asText();
                String role = suitableRole(requestedRole, step, plan.path("objective").asText());
                var profile = profiles.findByRole(role)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown agent role: " + role));
                var version = versions.findTopByProfileIdOrderByVersionDesc(profile.getId()).orElseThrow();
                stepRequests.add(new SprintStepRequest(step.path("stepKey").asText(), resolveBeadIds(step, beadIds), role, version.getId(),
                        uuids(step, "repositoryIds"), step.path("successCriteria").asText(), step.path("failurePolicy").asText(),
                        strings(step, "dependsOn"), strings(step, "resourceLocks")));
            }
            DeliveryPlanRequest deliveryPlanRequest = deliveryPlanRequest(plan, stepRequests, beadIds, relevantBeads, directive.getId());
            UUID deliveryPlanId = reconciliationMode
                    ? deliveryPlans.revise(currentPlan.getId(), deliveryPlanRequest, "project-manager").id()
                    : deliveryPlans.create(projectId, deliveryPlanRequest, "project-manager", "Project Manager").id();
            attempts.finish(attemptId, "SUCCEEDED", modelOutput, null, null);
            return deliveryPlanId;
        } catch (Exception exception) {
            attempts.finish(attemptId, "FAILED", modelOutput, "INVALID_PLAN", exception.getMessage());
            throw new IllegalStateException("Project manager returned invalid plan: " + exception.getMessage(), exception);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID projectId, String error) {
        assignments.findByProjectId(projectId).ifPresent(manager -> {
            manager.recordPlanningFailure(error, clock.instant());
            assignments.save(manager);
        });
    }

    private String extractJson(AgentRunResult result) {
        String text = result.events().stream().map(event -> extractTextPayload(event.payload())).reduce("", (a, b) -> a + b);
        int start = text.indexOf('{'); int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) throw new IllegalArgumentException("no JSON object in manager response");
        return text.substring(start, end + 1);
    }
    private String extractTextPayload(String payload) {
        try {
            JsonNode event = mapper.readTree(payload);
            JsonNode text = event.path("part").path("text");
            if (text.isTextual()) return text.asText();
            if (event.has("type")) return "";
        } catch (JsonProcessingException ignored) {
            // The runner may already provide a raw model response rather than an event envelope.
        }
        return payload;
    }
    static void validateGeneratedPlan(JsonNode plan) {
        if (plan.path("objective").asText().isBlank()) {
            throw new IllegalArgumentException("generated plan objective is blank");
        }
        JsonNode steps = plan.path("steps");
        if (!steps.isArray() || steps.isEmpty()) {
            throw new IllegalArgumentException("generated plan contains no steps");
        }
        for (JsonNode step : steps) {
            if (step.path("stepKey").asText().isBlank()) {
                throw new IllegalArgumentException("generated plan contains a step with no stepKey");
            }
            if (!step.path("beadIds").isArray() || step.path("beadIds").isEmpty()) {
                throw new IllegalArgumentException("generated plan step has no Beads: " + step.path("stepKey").asText());
            }
            for (JsonNode beadId : step.path("beadIds")) {
                if (beadId.asText().isBlank()) {
                    throw new IllegalArgumentException("generated plan step has a blank Bead reference: " + step.path("stepKey").asText());
                }
            }
        }
    }
    static String resolveSprintObjective(String managerObjective, String planObjective, String directiveObjective, String projectObjective) {
        for (String candidate : List.of(managerObjective, planObjective, directiveObjective, projectObjective)) {
            if (candidate != null && !candidate.isBlank()) return candidate.trim();
        }
        throw new IllegalArgumentException("sprint objective is blank: project has no objective or directive");
    }
    private List<String> strings(JsonNode node, String field) { List<String> values = new ArrayList<>(); node.withArray(field).forEach(v -> values.add(v.asText())); return values; }
    private List<String> resolveBeadIds(JsonNode step, Map<String, String> proposals) {
        return strings(step, "beadIds").stream().map(id -> proposals.getOrDefault(id, id)).toList();
    }
    private Map<String, String> createProposedBeads(JsonNode plan, String projectLabel, List<BeadsIssue> existing) {
        Map<String, String> resolved = new HashMap<>();
        Set<String> proposalKeys = new HashSet<>();
        Map<String, String> existingTitles = new HashMap<>();
        existing.forEach(issue -> existingTitles.put(issue.title(), issue.id()));
        for (JsonNode proposal : plan.withArray("proposedBeads")) {
            String key = proposal.path("proposalKey").asText();
            String title = proposal.path("title").asText();
            String description = proposal.path("description").asText();
            if (key.isBlank() || title.isBlank() || description.isBlank() || !proposalKeys.add(key)) {
                throw new IllegalArgumentException("Invalid or duplicate proposed Bead");
            }
            String id = existingTitles.get(title);
            if (id == null) {
                List<String> labels = new ArrayList<>();
                proposal.withArray("labels").forEach(label -> {
                    String value = label.asText();
                    if (!value.startsWith("project:")) labels.add(value);
                });
                labels.add(projectLabel);
                id = beads.createIssue(new BeadsClient.CreateBeadsIssue(title, description, labels)).id();
                existingTitles.put(title, id);
            }
            resolved.put(key, id);
        }
        return resolved;
    }
    private List<UUID> uuids(JsonNode node, String field) { return strings(node, field).stream().map(UUID::fromString).toList(); }
    private String suitableRole(String requested, JsonNode step, String objective) {
        String text = (objective + " " + step.path("stepKey").asText() + " " + step.path("successCriteria").asText()).toLowerCase();
        if (requested.equals("spec") && (text.contains("implement") || text.contains("build") || text.contains("compil"))) return "fullstack";
        if (requested.equals("spec") && (text.contains("test") || text.contains("check") || text.contains("verif"))) return "qa";
        return requested;
    }

    private DeliveryPlanRequest deliveryPlanRequest(JsonNode plan, List<SprintStepRequest> steps,
            Map<String, String> proposals, List<BeadsIssue> relevant, UUID directiveRevisionId) {
        Map<String, BeadsIssue> issues = new HashMap<>();
        relevant.forEach(issue -> issues.put(issue.id(), issue));
        List<DeliveryPlanBeadRequest> beadSnapshots = new ArrayList<>();
        Set<String> resolvedBeadIds = new HashSet<>(proposals.values());
        steps.forEach(step -> resolvedBeadIds.addAll(step.beadIds()));
        for (String beadId : resolvedBeadIds) {
            BeadsIssue issue = issues.get(beadId);
            if (issue == null) issue = beads.getIssue(beadId);
            try {
                beadSnapshots.add(new DeliveryPlanBeadRequest(issue.id(), null, issue.title(), issue.status(),
                        mapper.writeValueAsString(issue.dependencyIds()), mapper.writeValueAsString(issue.labels()),
                        mapper.writeValueAsString(issue)));
            } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException("Unable to snapshot Bead " + beadId, exception);
            }
        }
        List<DeliveryMilestoneRequest> milestones = new ArrayList<>();
        JsonNode supplied = plan.path("milestones");
        if (supplied.isArray()) {
            int sequence = 1;
            for (JsonNode milestone : supplied) {
                String key = milestone.path("milestoneKey").asText("milestone-" + sequence);
                milestones.add(new DeliveryMilestoneRequest(key, milestone.path("title").asText(key),
                        milestone.path("outcome").asText("Complete milestone " + key), sequence++));
            }
        }
        if (milestones.isEmpty()) {
            milestones.add(new DeliveryMilestoneRequest("milestone-1", plan.path("objective").asText(),
                    steps.stream().map(SprintStepRequest::successCriteria).reduce((left, right) -> left + "; " + right)
                            .orElse("Complete the planned work"), 1));
        }
        String acceptance = steps.stream().map(SprintStepRequest::successCriteria).reduce((left, right) -> left + "\n" + right)
                .orElse("Complete the DeliveryPlan objective");
        return new DeliveryPlanRequest(plan.path("objective").asText(), plan.path("reasoning").asText(), acceptance,
                plan.path("suggestedSprintBoundaries").asText("Generate one bounded sprint at a time from ready Beads"),
                normalizedPlanSnapshot(plan, proposals), milestones, beadSnapshots, directiveRevisionId);
    }

    private String normalizedPlanSnapshot(JsonNode plan, Map<String, String> proposals) {
        JsonNode copy = plan.deepCopy();
        for (JsonNode step : copy.withArray("steps")) {
            for (int index = 0; index < step.withArray("beadIds").size(); index++) {
                JsonNode bead = step.withArray("beadIds").get(index);
                ((com.fasterxml.jackson.databind.node.ArrayNode) step.withArray("beadIds")).set(index,
                        mapper.getNodeFactory().textNode(proposals.getOrDefault(bead.asText(), bead.asText())));
            }
        }
        return copy.toString();
    }
}
