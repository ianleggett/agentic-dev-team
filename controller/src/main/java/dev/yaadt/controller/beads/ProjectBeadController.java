package dev.yaadt.controller.beads;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/beads")
public class ProjectBeadController {
    private final ProjectBeadService service;

    public ProjectBeadController(ProjectBeadService service) { this.service = service; }

    @DeleteMapping("/{beadId}")
    public ResponseEntity<Void> remove(@PathVariable UUID projectId, @PathVariable String beadId,
            @RequestHeader(value = "X-User-Subject", defaultValue = "local-user") String subject,
            @RequestHeader(value = "X-User-Name", defaultValue = "Local User") String displayName) {
        service.removeFromProject(projectId, beadId, subject, displayName);
        return ResponseEntity.noContent().build();
    }
}
