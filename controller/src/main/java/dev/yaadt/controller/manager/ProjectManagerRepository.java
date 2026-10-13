package dev.yaadt.controller.manager;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectManagerRepository extends JpaRepository<ProjectManager, UUID> {
    Optional<ProjectManager> findByProjectId(UUID projectId);
}
