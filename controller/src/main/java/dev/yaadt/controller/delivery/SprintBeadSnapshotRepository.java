package dev.yaadt.controller.delivery;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SprintBeadSnapshotRepository extends JpaRepository<SprintBeadSnapshot, UUID> {
    List<SprintBeadSnapshot> findBySprintIdOrderByStepKeyAscBeadIdAsc(UUID sprintId);
}
