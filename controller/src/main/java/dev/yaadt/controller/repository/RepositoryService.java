package dev.yaadt.controller.repository;

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
public class RepositoryService {

    private final RepositoryRepository repositories;
    private final Clock clock;

    public RepositoryService(RepositoryRepository repositories, Clock clock) {
        this.repositories = repositories;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponse> list(UUID projectId) {
        return repositories.findByProjectIdOrderByName(projectId).stream().map(RepositoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RepositoryResponse get(UUID id) {
        return RepositoryResponse.from(find(id));
    }

    public Repository entity(UUID id) {
        return find(id);
    }

    public RepositoryResponse create(UUID projectId, RepositoryRequest request) {
        Instant now = Instant.now(clock);
        return RepositoryResponse.from(repositories.save(new Repository(
                projectId, request.name(), request.remoteUrl(), request.defaultBranch(), request.role(),
                request.relativeWorkspacePath(), request.buildCommands(), request.testCommands(),
                request.healthCheck(), now)));
    }

    public RepositoryResponse update(UUID id, RepositoryRequest request) {
        Repository repository = find(id);
        repository.update(request, Instant.now(clock));
        return RepositoryResponse.from(repository);
    }

    public void delete(UUID id) {
        repositories.delete(find(id));
    }

    private Repository find(UUID id) {
        return repositories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found"));
    }
}
