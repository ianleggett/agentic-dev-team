package dev.yaadt.controller.sprint;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/sprints")
public class SprintPlanController {
    private final SprintPlanService service;
    public SprintPlanController(SprintPlanService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<SprintResponse> create(@PathVariable UUID projectId, @Valid @RequestBody SprintPlanRequest request,
            @RequestHeader(value = "X-User-Subject", defaultValue = "local-user") String subject,
            @RequestHeader(value = "X-User-Name", defaultValue = "Local User") String displayName) {
        SprintResponse response = service.create(projectId, request, subject, displayName);
        return ResponseEntity.created(URI.create("/api/projects/" + projectId + "/sprints/" + response.id())).body(response);
    }

    @PostMapping("/manual")
    public ResponseEntity<SprintResponse> createManual(@PathVariable UUID projectId,
            @Valid @RequestBody ManualSprintRequest request,
            @RequestHeader(value = "X-User-Subject", defaultValue = "local-user") String subject,
            @RequestHeader(value = "X-User-Name", defaultValue = "Local User") String displayName) {
        SprintResponse response = service.createManual(projectId, request, subject, displayName);
        return ResponseEntity.created(URI.create("/api/projects/" + projectId + "/sprints/" + response.id())).body(response);
    }
}
