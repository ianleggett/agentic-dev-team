package dev.yaadt.controller.directive;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectiveRepository extends JpaRepository<DirectiveRevision, UUID> {
    Optional<DirectiveRevision> findTopByProjectIdOrderByVersionDesc(UUID projectId);
}
