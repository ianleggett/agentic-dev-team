package dev.yaadt.controller.sprint;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SprintCommandIdempotencyRepository extends JpaRepository<SprintCommandIdempotency, UUID> {
    Optional<SprintCommandIdempotency> findByKey(String key);
}
