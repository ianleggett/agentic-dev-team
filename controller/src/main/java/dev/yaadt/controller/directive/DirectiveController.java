package dev.yaadt.controller.directive;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/directives")
public class DirectiveController {
    private final DirectiveService service;
    public DirectiveController(DirectiveService service) { this.service = service; }
    @org.springframework.web.bind.annotation.GetMapping
    public List<DirectiveResponse> list(@PathVariable UUID projectId) { return service.list(projectId); }
    @PostMapping
    public ResponseEntity<DirectiveResponse> create(@PathVariable UUID projectId, @Valid @RequestBody DirectiveRequest request,
            @RequestHeader(value = "X-User-Subject", defaultValue = "local-user") String subject,
            @RequestHeader(value = "X-User-Name", defaultValue = "Local User") String name) {
        DirectiveResponse response = service.create(projectId, request, subject, name);
        return ResponseEntity.created(URI.create("/api/projects/" + projectId + "/directives/" + response.id())).body(response);
    }
}
