package dev.yaadt.controller.sprint;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SprintPlanVersionRepository extends JpaRepository<SprintPlanVersion, UUID> {
    Optional<SprintPlanVersion> findTopBySprintIdOrderByVersionDesc(UUID sprintId);
}
