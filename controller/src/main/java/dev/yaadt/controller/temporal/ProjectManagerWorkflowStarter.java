package dev.yaadt.controller.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowNotFoundException;
import dev.yaadt.controller.delivery.DeliveryPlanRepository;
import dev.yaadt.controller.delivery.DeliveryPlanSchedulerService;
import dev.yaadt.controller.delivery.DeliveryPlanService;
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
    private final DeliveryPlanRepository deliveryPlans;
    private final DeliveryPlanService deliveryPlanService;
    private final DeliveryPlanSchedulerService scheduler;
    private final TemporalWorkflowStarter sprintStarter;
    private final ProjectManagerService managers;
    private final ProjectManagerPlanningAttemptService attempts;

    public ProjectManagerWorkflowStarter(WorkflowClient client, TemporalProperties properties, DeliveryPlanRepository deliveryPlans,
            DeliveryPlanService deliveryPlanService, DeliveryPlanSchedulerService scheduler, TemporalWorkflowStarter sprintStarter,
            ProjectManagerService managers, ProjectManagerPlanningAttemptService attempts) {
        this.client = client; this.properties = properties; this.deliveryPlans = deliveryPlans;
        this.deliveryPlanService = deliveryPlanService; this.scheduler = scheduler;
        this.sprintStarter = sprintStarter; this.managers = managers; this.attempts = attempts;
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
        var deliveryPlan = deliveryPlans.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .filter(candidate -> candidate.getState().equals("DRAFT") || candidate.getState().equals("PENDING_APPROVAL"))
                .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No DeliveryPlan is awaiting Project Manager approval"));
        String workflowId = "yaadt-project-manager-" + projectId;
        var workflow = client.newWorkflowStub(TemporalWorkflowContracts.ProjectManagerWorkflow.class, workflowId);
        try {
            workflow.approve();
            return ResponseEntity.accepted().body(Map.of("workflowId", workflowId, "state", "APPROVED"));
        } catch (WorkflowNotFoundException completed) {
            if ("DRAFT".equals(deliveryPlan.getState())) deliveryPlanService.submit(deliveryPlan.getId());
            deliveryPlanService.approve(deliveryPlan.getId(),
                    new dev.yaadt.controller.delivery.DeliveryPlanDecisionRequest(deliveryPlan.getCurrentVersion(), "Project Manager approval"),
                    "project-manager");
            var sprint = scheduler.generateNextSprint(deliveryPlan.getId(), "project-manager", "Project Manager");
            return sprintStarter.start(sprint.id());
        }
    }
}
