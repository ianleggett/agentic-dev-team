package dev.yaadt.controller.directive;

import dev.yaadt.controller.user.CurrentUserService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DirectiveService {
    private final DirectiveRepository directives;
    private final CurrentUserService users;
    private final Clock clock;
    public DirectiveService(DirectiveRepository directives, CurrentUserService users, Clock clock) {
        this.directives = directives; this.users = users; this.clock = clock;
    }
    public DirectiveResponse create(UUID projectId, DirectiveRequest request, String subject, String name) {
        int next = directives.findTopByProjectIdOrderByVersionDesc(projectId).map(d -> d.getVersion() + 1).orElse(1);
        return DirectiveResponse.from(directives.save(new DirectiveRevision(projectId, next, request.content(),
                users.resolve(subject, name).getId(), Instant.now(clock))));
    }
    @Transactional(readOnly = true)
    public List<DirectiveResponse> list(UUID projectId) {
        return directives.findByProjectIdOrderByVersionDesc(projectId).stream().map(DirectiveResponse::from).toList();
    }
}
