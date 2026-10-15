package dev.yaadt.controller.manager;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface ControllerLeaseRepository extends JpaRepository<ControllerLease, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ControllerLease> findLockedByLeaseKey(String leaseKey);
}
