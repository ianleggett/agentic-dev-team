package dev.yaadt.controller.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yaadt.controller.beads.BeadsClient;
import dev.yaadt.controller.beads.BeadsCapabilities;
import dev.yaadt.controller.beads.BeadsIssue;
import dev.yaadt.controller.beads.BeadsProjectLabels;
import dev.yaadt.controller.beads.BeadsStatus;
import dev.yaadt.controller.profile.AgentProfileRepository;
import dev.yaadt.controller.profile.AgentProfileVersionRepository;
import dev.yaadt.controller.project.ProjectRepository;
import dev.yaadt.controller.sprint.Sprint;
import dev.yaadt.controller.sprint.SprintPlanRequest;
import dev.yaadt.controller.sprint.SprintPlanService;
import dev.yaadt.controller.sprint.SprintRepository;
import dev.yaadt.controller.sprint.SprintResponse;
import dev.yaadt.controller.sprint.SprintStepRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class DeliveryPlanSchedulerService {
    private final DeliveryPlanRepository plans;
    private final DeliveryPlanVersionRepository versions;
    private final SprintRepository sprints;
    private final SprintPlanService sprintPlans;
    private final ProjectRepository projects;
    private final AgentProfileRepository profiles;
    private final AgentProfileVersionRepository profileVersions;
    private final SprintBeadSnapshotRepository beadSnapshots;
    private final BeadsClient beads;
    private final ObjectMapper mapper;
    private final Clock clock;

    public DeliveryPlanSchedulerService(DeliveryPlanRepository plans, DeliveryPlanVersionRepository versions,
            SprintRepository sprints, SprintPlanService sprintPlans, ProjectRepository projects,
            AgentProfileRepository profiles, AgentProfileVersionRepository profileVersions, SprintBeadSnapshotRepository beadSnapshots,
            BeadsClient beads,
            ObjectMapper mapper, Clock clock) {
        this.plans = plans; this.versions = versions; this.sprints = sprints; this.sprintPlans = sprintPlans;
        this.projects = projects; this.profiles = profiles; this.profileVersions = profileVersions;
        this.beadSnapshots = beadSnapshots; this.beads = beads;
        this.mapper = mapper; this.clock = clock;
    }

    public SprintResponse generateNextSprint(UUID planId, String subject, String displayName) {
        DeliveryPlan plan = plans.findForUpdateById(planId).orElseThrow(() -> notFound("DeliveryPlan not found"));
        if (!"APPROVED".equals(plan.getState())) conflict("Only an approved DeliveryPlan can generate a Sprint");
        if (sprints.findByDeliveryPlanIdOrderByCreatedAtDesc(planId).stream().anyMatch(this::active)) {
            conflict("DeliveryPlan already has an active Sprint");
        }
        DeliveryPlanVersion version = versions.findByDeliveryPlanIdAndVersion(planId, plan.getApprovedVersion()).orElseThrow(() ->
                notFound("Approved DeliveryPlan version not found"));
        if (version.getDirectiveRevisionId() == null) conflict("DeliveryPlan has no directive revision");
        var project = projects.findById(plan.getProjectId()).orElseThrow(() -> notFound("Project not found"));
        try {
            BeadsCapabilities capabilities = beads.capabilities();
            if (capabilities != null && !capabilities.available()) {
                conflict("Beads is unavailable: " + capabilities.error());
            }
            if (capabilities != null && !capabilities.jsonOutput()) {
                conflict("Beads CLI does not support JSON output");
            }
            JsonNode snapshot = mapper.readTree(version.getPlanSnapshot());
            Map<String, List<String>> stepBeads = stepBeads(snapshot);
            Map<String, BeadsIssue> live = beads.listRelevantIssues(BeadsProjectLabels.forProject(project.getBeadsProject())).stream()
                    .collect(java.util.stream.Collectors.toMap(BeadsIssue::id, issue -> issue, (left, right) -> left));
            List<SprintStepRequest> selected = new ArrayList<>();
            for (JsonNode step : snapshot.withArray("steps")) {
                List<String> beadIds = strings(step, "beadIds");
                boolean beadsReady = beadIds.stream().allMatch(id -> live.containsKey(id) && !BeadsStatus.isTerminal(live.get(id).status()));
                boolean dependenciesReady = strings(step, "dependsOn").stream()
                        .flatMap(key -> stepBeads.getOrDefault(key, List.of()).stream())
                        .allMatch(id -> live.containsKey(id) && BeadsStatus.isTerminal(live.get(id).status()));
                if (!beadsReady || !dependenciesReady) continue;
                String role = step.path("agentRole").asText();
                var profile = profiles.findByRole(role).orElseThrow(() -> conflictException("Unknown agent role: " + role));
                var profileVersion = profileVersions.findTopByProfileIdOrderByVersionDesc(profile.getId()).orElseThrow(() ->
                        conflictException("Agent profile version not found for role: " + role));
                selected.add(new SprintStepRequest(step.path("stepKey").asText(), beadIds, role, profileVersion.getId(),
                        uuids(step, "repositoryIds"), step.path("successCriteria").asText(), step.path("failurePolicy").asText("stop"),
                        strings(step, "dependsOn"), strings(step, "resourceLocks")));
            }
            if (selected.isEmpty()) conflict("No ready Beads are available for the next Sprint");
            Set<String> selectedKeys = selected.stream().map(SprintStepRequest::stepKey).collect(java.util.stream.Collectors.toSet());
            selected = selected.stream().map(step -> new SprintStepRequest(step.stepKey(), step.beadIds(), step.agentRole(),
                    step.agentProfileVersionId(), step.repositoryIds(), step.successCriteria(), step.failurePolicy(),
                    step.dependsOn().stream().filter(selectedKeys::contains).toList(), step.resourceLocks())).toList();
            SprintPlanRequest request = new SprintPlanRequest(plan.getObjective(), version.getDirectiveRevisionId(),
                    version.getReasoning(), snapshot.path("approvalPolicy").asText("human"), snapshot.path("failurePolicy").asText("stop"), selected);
            SprintResponse response = sprintPlans.create(plan.getProjectId(), request, subject, displayName);
            Sprint sprint = sprints.findById(response.id()).orElseThrow(() -> notFound("Generated Sprint not found"));
            sprint.bindDeliveryPlan(planId, version.getId(), firstMilestone(snapshot), Instant.now(clock));
            sprints.save(sprint);
            for (SprintStepRequest step : selected) {
                for (String beadId : step.beadIds()) {
                    BeadsIssue issue = live.get(beadId);
                    beadSnapshots.save(new SprintBeadSnapshot(sprint.getId(), beadId, step.stepKey(), issue.title(),
                            issue.status(), mapper.writeValueAsString(issue.dependencyIds()), mapper.writeValueAsString(issue.labels()),
                            mapper.writeValueAsString(issue), "Selected as live ready work for step " + step.stepKey()));
                }
            }
            return response;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid DeliveryPlan snapshot", exception);
        }
    }

    private boolean active(Sprint sprint) {
        return Set.of("DRAFT", "PLANNED", "WAITING_FOR_APPROVAL", "READY", "RUNNING", "PAUSED", "UNKNOWN", "RECOVERY_REQUIRED")
                .contains(sprint.getState());
    }

    private Map<String, List<String>> stepBeads(JsonNode snapshot) {
        Map<String, List<String>> result = new HashMap<>();
        for (JsonNode step : snapshot.withArray("steps")) result.put(step.path("stepKey").asText(), strings(step, "beadIds"));
        return result;
    }

    private List<String> strings(JsonNode node, String field) {
        List<String> values = new ArrayList<>();
        node.withArray(field).forEach(value -> values.add(value.asText()));
        return values;
    }

    private List<UUID> uuids(JsonNode node, String field) {
        return strings(node, field).stream().map(UUID::fromString).toList();
    }

    private String firstMilestone(JsonNode snapshot) {
        JsonNode milestones = snapshot.path("milestones");
        return milestones.isArray() && !milestones.isEmpty() ? milestones.get(0).path("milestoneKey").asText(null) : null;
    }

    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private void conflict(String message) { throw conflictException(message); }
    private ResponseStatusException conflictException(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
