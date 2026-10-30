package dev.yaadt.controller.beads;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeadsMutationOutboxRepository extends JpaRepository<BeadsMutationOutbox, UUID> {
    Optional<BeadsMutationOutbox> findByMutationKey(String mutationKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<BeadsMutationOutbox> findTop20ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(String status, Instant now);
}
