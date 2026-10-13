package dev.yaadt.controller.manager;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import dev.yaadt.controller.profile.AgentProfileVersionRepository;

@Service
@Transactional
public class ProjectManagerService {
    private final ProjectManagerRepository managers;
    private final AgentProfileVersionRepository profiles;
    private final Clock clock;
    public ProjectManagerService(ProjectManagerRepository managers, AgentProfileVersionRepository profiles, Clock clock) {
        this.managers = managers; this.profiles = profiles; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public ProjectManagerResponse get(UUID projectId) {
        return managers.findByProjectId(projectId).map(ProjectManagerResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project manager not assigned"));
    }
    public ProjectManagerResponse assign(UUID projectId, ManagerAssignmentRequest request) {
        profiles.findById(request.agentProfileVersionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent profile version not found"));
        ProjectManager manager = managers.findByProjectId(projectId)
                .orElseGet(() -> new ProjectManager(projectId, request.agentProfileVersionId(), Instant.now(clock)));
        manager.assign(request.agentProfileVersionId(), Instant.now(clock));
        return ProjectManagerResponse.from(managers.save(manager));
    }
}
