package dev.yaadt.controller.profile;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent-profiles")
public class AgentProfileController {
    private final AgentProfileService service;

    public AgentProfileController(AgentProfileService service) { this.service = service; }

    @GetMapping
    public List<AgentProfileResponse> list() { return service.list(); }

    @PostMapping
    public ResponseEntity<AgentProfileResponse> create(@Valid @RequestBody AgentProfileRequest request,
            @RequestHeader(value = "X-User-Subject", defaultValue = "local-user") String subject,
            @RequestHeader(value = "X-User-Name", defaultValue = "Local User") String displayName) {
        AgentProfileResponse response = service.create(request, subject, displayName);
        return ResponseEntity.created(URI.create("/api/agent-profiles/" + response.id())).body(response);
    }

    @PostMapping("/{id}/versions")
    public AgentProfileResponse addVersion(@PathVariable UUID id, @Valid @RequestBody AgentProfileRequest request,
            @RequestHeader(value = "X-User-Subject", defaultValue = "local-user") String subject,
            @RequestHeader(value = "X-User-Name", defaultValue = "Local User") String displayName) {
        return service.addVersion(id, request, subject, displayName);
    }
}
