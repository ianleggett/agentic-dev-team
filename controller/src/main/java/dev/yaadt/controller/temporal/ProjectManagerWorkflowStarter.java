package dev.yaadt.controller.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowNotFoundException;
import dev.yaadt.controller.sprint.SprintCommandService;
import dev.yaadt.controller.sprint.SprintPlanVersionRepository;
import dev.yaadt.controller.sprint.SprintRepository;
import dev.yaadt.controller.sprint.SprintCommandRequest;
import dev.yaadt.controller.manager.ProjectManagerService;
import dev.yaadt.controller.manager.ProjectManagerPlanningAttemptService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projects/{projectId}/project-manager")
public class ProjectManagerWorkflowStarter {
    private final WorkflowClient client;
    private final TemporalProperties properties;
    private final SprintRepository sprints;
    private final SprintPlanVersionRepository plans;
    private final SprintCommandService commands;
    private final TemporalWorkflowStarter sprintStarter;
    private final ProjectManagerService managers;
    private final ProjectManagerPlanningAttemptService attempts;

    public ProjectManagerWorkflowStarter(WorkflowClient client, TemporalProperties properties, SprintRepository sprints,
            SprintPlanVersionRepository plans, SprintCommandService commands, TemporalWorkflowStarter sprintStarter,
            ProjectManagerService managers, ProjectManagerPlanningAttemptService attempts) {
        this.client = client; this.properties = properties; this.sprints = sprints; this.plans = plans;
        this.commands = commands; this.sprintStarter = sprintStarter; this.managers = managers; this.attempts = attempts;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> start(@PathVariable UUID projectId, @Valid @RequestBody ProjectManagerStartRequest request) {
        managers.setPlanningObjective(projectId, request.objective());
        managers.clearPlanningFailure(projectId);
        String workflowId = "yaadt-project-manager-" + projectId;
        var workflow = client.newWorkflowStub(TemporalWorkflowContracts.ProjectManagerWorkflow.class,
                WorkflowOptions.newBuilder().setWorkflowId(workflowId).setTaskQueue(properties.taskQueue()).build());
        try {
            WorkflowClient.start(workflow::run, projectId.toString());
        } catch (WorkflowExecutionAlreadyStarted alreadyRunning) {
            return ResponseEntity.accepted().body(Map.of("workflowId", workflowId, "state", "ALREADY_PLANNING"));
        }
        return ResponseEntity.accepted().body(Map.of("workflowId", workflowId, "state", "PLANNING"));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, String>> cancel(@PathVariable UUID projectId) {
        String workflowId = "yaadt-project-manager-" + projectId;
        var workflow = client.newWorkflowStub(TemporalWorkflowContracts.ProjectManagerWorkflow.class, workflowId);
        try {
            io.temporal.client.WorkflowStub.fromTyped(workflow).cancel();
        } catch (WorkflowNotFoundException ignored) {
            // The workflow may have completed between the status check and cancellation.
        }
        attempts.cancelLatest(projectId);
        return ResponseEntity.accepted().body(Map.of("workflowId", workflowId, "state", "CANCEL_REQUESTED"));
    }

    @PostMapping("/approve")
    public ResponseEntity<Map<String, String>> approve(@PathVariable UUID projectId) {
        boolean draftExists = sprints.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .anyMatch(candidate -> candidate.getState().equals("DRAFT"));
        if (!draftExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No draft sprint plan is awaiting Project Manager approval");
        }
        String workflowId = "yaadt-project-manager-" + projectId;
        var workflow = client.newWorkflowStub(TemporalWorkflowContracts.ProjectManagerWorkflow.class, workflowId);
        try {
            workflow.approve();
            return ResponseEntity.accepted().body(Map.of("workflowId", workflowId, "state", "APPROVED"));
        } catch (WorkflowNotFoundException completed) {
            var sprint = sprints.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                    .filter(candidate -> candidate.getState().equals("DRAFT"))
                    .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Project-manager workflow is complete and no draft sprint plan is awaiting approval"));
            int version = plans.findTopBySprintIdOrderByVersionDesc(sprint.getId()).orElseThrow().getVersion();
            commands.command(sprint.getId(), "approve", new SprintCommandRequest(version, "project manager approval"),
                    "project-manager", "Project Manager", "project-manager-approval:" + sprint.getId() + ":" + version);
            return sprintStarter.start(sprint.getId());
        }
    }
}
