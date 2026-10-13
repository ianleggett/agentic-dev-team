package dev.yaadt.controller.sprint;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SprintStepRepository extends JpaRepository<SprintStep, UUID> {
    List<SprintStep> findByPlanVersionIdOrderByStepKey(UUID planVersionId);
}
