package dev.yaadt.controller.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import dev.yaadt.controller.audit.AuditEvent;
import dev.yaadt.controller.audit.AuditEventRepository;
import dev.yaadt.controller.sprint.Sprint;
import dev.yaadt.controller.sprint.SprintRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/sprints")
public class TemporalWorkflowStarter {
    private final WorkflowClient client;
    private final TemporalProperties properties;
    private final SprintRepository sprints;
    private final AuditEventRepository audit;
    private final Clock clock;
    public TemporalWorkflowStarter(WorkflowClient client, TemporalProperties properties, SprintRepository sprints,
            AuditEventRepository audit, Clock clock) {
        this.client = client; this.properties = properties; this.sprints = sprints; this.audit = audit; this.clock = clock;
    }
    @PostMapping("/{sprintId}/start")
    public ResponseEntity<Map<String, String>> start(@PathVariable UUID sprintId) {
        Sprint sprint = sprints.findById(sprintId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found"));
        if (!sprint.getState().equals("READY")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sprint must be READY before it can start");
        }
        Instant now = Instant.now(clock);
        sprint.transition("RUNNING", now);
        audit.save(new AuditEvent("SYSTEM", "temporal", sprintId, "start", "READY", "RUNNING", "workflow started", now));
        String workflowId = "yaadt-sprint-" + sprintId;
        TemporalWorkflowContracts.SprintWorkflow workflow = client.newWorkflowStub(
                TemporalWorkflowContracts.SprintWorkflow.class,
                WorkflowOptions.newBuilder().setWorkflowId(workflowId).setTaskQueue(properties.taskQueue()).build());
        WorkflowClient.start(workflow::run, sprintId.toString());
        return ResponseEntity.accepted().body(Map.of("workflowId", workflowId, "state", "RUNNING"));
    }
}
