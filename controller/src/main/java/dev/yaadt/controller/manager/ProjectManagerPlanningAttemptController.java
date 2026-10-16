package dev.yaadt.controller.manager;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/manager/planning-attempts")
public class ProjectManagerPlanningAttemptController {
    private final ProjectManagerPlanningAttemptService service;
    public ProjectManagerPlanningAttemptController(ProjectManagerPlanningAttemptService service) { this.service = service; }
    @GetMapping
    public List<ProjectManagerPlanningAttemptResponse> list(@PathVariable UUID projectId) { return service.list(projectId); }
}
