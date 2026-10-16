package dev.yaadt.controller.manager;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectManagerPlanningAttemptRepository extends JpaRepository<ProjectManagerPlanningAttempt, UUID> {
    List<ProjectManagerPlanningAttempt> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
