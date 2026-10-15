package dev.yaadt.controller.manager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ControllerLeaseService {
    private static final Duration LEASE_DURATION = Duration.ofMinutes(45);
    private final ControllerLeaseRepository leases;
    private final Clock clock;

    public ControllerLeaseService(ControllerLeaseRepository leases, Clock clock) {
        this.leases = leases;
        this.clock = clock;
    }

    public String acquire(String leaseKey, String ownerId) {
        Instant now = Instant.now(clock);
        ControllerLease lease = leases.findLockedByLeaseKey(leaseKey).orElse(null);
        if (lease != null && lease.getExpiresAt().isAfter(now) && !lease.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lease is held: " + leaseKey);
        }
        if (lease == null) {
            lease = new ControllerLease(leaseKey, ownerId, now.plus(LEASE_DURATION), now);
        } else {
            lease.renew(ownerId, now.plus(LEASE_DURATION), now);
        }
        leases.save(lease);
        return ownerId;
    }

    public String heartbeat(String leaseKey, String ownerId) {
        return acquire(leaseKey, ownerId);
    }

    public void release(String leaseKey, String ownerId) {
        leases.findLockedByLeaseKey(leaseKey).filter(lease -> lease.getOwnerId().equals(ownerId))
                .ifPresent(lease -> leases.delete(lease));
    }
}
