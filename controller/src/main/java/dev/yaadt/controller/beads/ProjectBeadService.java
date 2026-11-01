package dev.yaadt.controller.beads;

import dev.yaadt.controller.project.Project;
import dev.yaadt.controller.project.ProjectRepository;
import dev.yaadt.controller.user.CurrentUserService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ProjectBeadService {
    private final ProjectRepository projects;
    private final BeadsClient beads;
    private final CurrentUserService users;

    public ProjectBeadService(ProjectRepository projects, BeadsClient beads, CurrentUserService users) {
        this.projects = projects;
        this.beads = beads;
        this.users = users;
    }

    public void removeFromProject(UUID projectId, String beadId, String subject, String displayName) {
        Project project = projects.findById(projectId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        String label = BeadsProjectLabels.forProject(project.getName());
        BeadsIssue issue = beads.getIssue(beadId);
        if (!issue.labels().contains(label)) {
            return; // Disassociation is idempotent; the live Bead may already have lost this label.
        }
        String actor = users.resolve(subject, displayName).getSubject();
        beads.addComment(beadId, "Disassociated from YAADT project " + project.getName() + " by " + actor);
        beads.removeLabel(beadId, label);
    }
}
