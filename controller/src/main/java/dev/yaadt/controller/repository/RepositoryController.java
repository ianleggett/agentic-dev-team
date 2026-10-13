package dev.yaadt.controller.repository;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/repositories")
public class RepositoryController {

    private final RepositoryService service;

    public RepositoryController(RepositoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<RepositoryResponse> list(@PathVariable UUID projectId) {
        return service.list(projectId);
    }

    @GetMapping("/{id}")
    public RepositoryResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<RepositoryResponse> create(
            @PathVariable UUID projectId, @Valid @RequestBody RepositoryRequest request) {
        RepositoryResponse response = service.create(projectId, request);
        return ResponseEntity.created(URI.create("/api/projects/" + projectId + "/repositories/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    public RepositoryResponse update(@PathVariable UUID id, @Valid @RequestBody RepositoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
