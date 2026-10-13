package dev.yaadt.controller.manager;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/manager")
public class ProjectManagerController {
    private final ProjectManagerService service;
    public ProjectManagerController(ProjectManagerService service) { this.service = service; }
    @GetMapping
    public ProjectManagerResponse get(@PathVariable UUID projectId) { return service.get(projectId); }
    @PutMapping
    public ProjectManagerResponse assign(@PathVariable UUID projectId, @Valid @RequestBody ManagerAssignmentRequest request) {
        return service.assign(projectId, request);
    }
}
