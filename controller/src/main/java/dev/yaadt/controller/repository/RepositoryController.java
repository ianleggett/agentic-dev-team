package dev.yaadt.controller.repository;

import jakarta.validation.Valid;
import dev.yaadt.controller.executor.GitCheckoutExecutor;
import dev.yaadt.controller.executor.GitCheckoutRequest;
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
    private final GitCheckoutExecutor checkoutExecutor;

    public RepositoryController(RepositoryService service, GitCheckoutExecutor checkoutExecutor) {
        this.service = service;
        this.checkoutExecutor = checkoutExecutor;
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

    @PostMapping("/{id}/checkout")
    public RepositoryCheckoutResponse checkout(@PathVariable UUID projectId, @PathVariable UUID id) {
        Repository repository = service.entity(id);
        if (!repository.getProjectId().equals(projectId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Repository not found");
        }
        return RepositoryCheckoutResponse.from(checkoutExecutor.checkout(new GitCheckoutRequest(
                repository.getId(), repository.getRemoteUrl(), repository.getDefaultBranch(),
                repository.getRelativeWorkspacePath())));
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
