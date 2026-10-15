package dev.yaadt.controller.sprint;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SprintCommandRepository extends JpaRepository<SprintCommand, UUID> {
    Optional<SprintCommand> findBySprintIdAndIdempotencyKey(UUID sprintId, String idempotencyKey);
}
