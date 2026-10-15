package dev.yaadt.controller.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yaadt.controller.audit.SprintNarrativeService;
import dev.yaadt.controller.beads.BeadsClient;
import dev.yaadt.controller.beads.BeadsIssue;
import dev.yaadt.controller.beads.BeadsProjectLabels;
import dev.yaadt.controller.directive.DirectiveRepository;
import dev.yaadt.controller.executor.AgentExecutor;
import dev.yaadt.controller.executor.AgentBackendConfig;
import dev.yaadt.controller.executor.AgentRunRequest;
import dev.yaadt.controller.executor.AgentRunResult;
import dev.yaadt.controller.profile.AgentProfileRepository;
import dev.yaadt.controller.profile.AgentProfileVersionRepository;
import dev.yaadt.controller.project.ProjectRepository;
import dev.yaadt.controller.repository.RepositoryRepository;
import dev.yaadt.controller.sprint.SprintPlanRequest;
import dev.yaadt.controller.sprint.SprintPlanService;
import dev.yaadt.controller.sprint.SprintStepRequest;
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

@Service
public class ProjectManagerPlanningService {
    private final ProjectRepository projects;
    private final RepositoryRepository repositories;
    private final DirectiveRepository directives;
    private final AgentProfileRepository profiles;
    private final AgentProfileVersionRepository versions;
    private final ProjectManagerRepository assignments;
    private final SprintPlanService plans;
    private final AgentExecutor executor;
    private final ObjectMapper mapper;
    private final SprintNarrativeService narrative;
    private final Clock clock;
    private final BeadsClient beads;

    public ProjectManagerPlanningService(ProjectRepository projects, RepositoryRepository repositories,
            DirectiveRepository directives, AgentProfileRepository profiles, AgentProfileVersionRepository versions,
            ProjectManagerRepository assignments, SprintPlanService plans, AgentExecutor executor, ObjectMapper mapper,
            SprintNarrativeService narrative, Clock clock, BeadsClient beads) {
        this.projects = projects; this.repositories = repositories; this.directives = directives; this.profiles = profiles;
        this.assignments = assignments;
        this.versions = versions; this.plans = plans; this.executor = executor; this.mapper = mapper;
        this.narrative = narrative; this.clock = clock; this.beads = beads;
    }

    @Transactional
    public UUID generate(UUID projectId) {
        var project = projects.findById(projectId).orElseThrow(() -> new IllegalArgumentException("Project not found"));
        var directive = directives.findTopByProjectIdOrderByVersionDesc(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project has no directive"));
        var managerVersion = assignments.findByProjectId(projectId)
                .map(a -> versions.findById(a.getAgentProfileVersionId()).orElse(null)).orElse(null);
        if (managerVersion == null) {
            var manager = profiles.findByRole("project-manager").orElseThrow(() -> new IllegalArgumentException("Project manager profile not found"));
            managerVersion = versions.findTopByProfileIdOrderByVersionDesc(manager.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Project manager profile version not found"));
        }
        String projectLabel = BeadsProjectLabels.forProject(project.getBeadsProject());
        List<BeadsIssue> relevantBeads = beads.listRelevantIssues(projectLabel);
        String beadsContext = relevantBeads.stream()
                .map(issue -> "id=" + issue.id() + " status=" + issue.status() + " labels=" + issue.labels()
                        + " dependsOn=" + issue.dependencyIds() + " title=" + issue.title() + " description=" + issue.description())
                .reduce("", (a, b) -> a + b + "\n");
        String context = "PROJECT\nname=" + project.getName() + " beadsProject=" + project.getBeadsProject()
                + " beadsProjectLabel=" + projectLabel
                + " defaultBranch=" + project.getDefaultBranch() + "\nDIRECTIVE\n" + directive.getContent()
                + "\nREPOSITORIES\n" + repositories.findByProjectIdOrderByName(projectId).stream()
                .map(r -> r.getId() + " name=" + r.getName() + " role=" + r.getRole() + " path=" + r.getRelativeWorkspacePath()
                        + " build=" + r.getBuildCommands() + " test=" + r.getTestCommands())
                .reduce("", (a, b) -> a + b + "\n")
                + "\nRELEVANT BEADS\n" + beadsContext;
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
        String prompt = managerVersion.getTeamPrompt() + "\n\nCreate a sprint plan from this context. Use only beadIds from RELEVANT BEADS, or proposalKey values from proposedBeads for current gaps. Return JSON only with fields objective, reasoning, approvalPolicy, failurePolicy, proposedBeads, and steps. proposedBeads must contain proposalKey, title, description, and labels. Each step must contain stepKey, beadIds, agentRole, repositoryIds, successCriteria, failurePolicy, dependsOn, and resourceLocks. Use only roles from the catalog. Assign implementation/build/compile-fixing work to fullstack (or database/devops when the repository task is specifically those domains), and assign verification/testing work to qa. Never assign spec to implementation or compilation work.\n\n" + catalog + "\n" + context;
        AgentRunResult result = executor.execute(new AgentRunRequest("project-manager:" + projectId + ":" + clock.instant(), UUID.randomUUID(), 1,
                "/workspace/runs/project-manager-" + projectId, prompt, java.util.Map.of(),
                new AgentBackendConfig(managerVersion.getProviderIdentifier(), managerVersion.getModelIdentifier(), managerVersion.getBaseUrl(), managerVersion.getApiKey())));
        if (result.state() != AgentRunResult.State.SUCCEEDED) throw new IllegalStateException(result.errorCode() + ": " + result.errorMessage());
        try {
            JsonNode plan = mapper.readTree(extractJson(result));
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
            SprintPlanRequest request = new SprintPlanRequest(plan.path("objective").asText(), directive.getId(), plan.path("reasoning").asText(),
                    plan.path("approvalPolicy").asText("human"), plan.path("failurePolicy").asText("stop"), stepRequests);
            UUID sprintId = plans.create(projectId, request, "project-manager", "Project Manager").id();
            narrative.record(sprintId, "manager-plan-created", "DRAFT", "project-manager generated and assigned " + stepRequests.size()
                    + " steps and created " + beadIds.size() + " gap Beads; awaiting approval");
            return sprintId;
        } catch (Exception exception) {
            throw new IllegalStateException("Project manager returned invalid plan: " + exception.getMessage(), exception);
        }
    }

    private String extractJson(AgentRunResult result) {
        String text = result.events().stream().map(e -> e.payload()).reduce("", (a, b) -> a + b);
        int start = text.indexOf('{'); int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) throw new IllegalArgumentException("no JSON object in manager response");
        return text.substring(start, end + 1);
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
}
