package dev.yaadt.controller.project;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository repository;
    private final Clock clock;

    public ProjectService(ProjectRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return repository.findAll().stream().map(ProjectResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID id) {
        return ProjectResponse.from(find(id));
    }

    public ProjectResponse create(ProjectRequest request) {
        Project project = new Project(request.name(), request.objective() == null ? "" : request.objective(), request.beadsProject(), request.workspaceProvider());
        project.touch(Instant.now(clock));
        return ProjectResponse.from(repository.save(project));
    }

    public ProjectResponse update(UUID id, ProjectRequest request) {
        Project project = find(id);
        project.update(
                request.name(),
                request.objective() == null || request.objective().isBlank() ? project.getObjective() : request.objective(),
                request.beadsProject(),
                request.workspaceProvider(),
                request.active() == null || request.active(),
                valueOrDefault(request.defaultBranch(), project.getDefaultBranch()),
                valueOrDefault(request.executionMode(), project.getExecutionMode()),
                request.maxConcurrentAgentRuns() == null
                        ? project.getMaxConcurrentAgentRuns() : request.maxConcurrentAgentRuns(),
                request.maxConcurrentRunsPerProject() == null
                        ? project.getMaxConcurrentRunsPerProject() : request.maxConcurrentRunsPerProject(),
                request.approvalRequired() == null ? project.isApprovalRequired() : request.approvalRequired());
        project.touch(Instant.now(clock));
        return ProjectResponse.from(project);
    }

    public void delete(UUID id) {
        Project project = find(id);
        repository.delete(project);
    }

    private Project find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
