package dev.yaadt.controller.manager;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "controller_leases")
public class ControllerLease {
    @Id
    @Column(name = "lease_key", length = 240)
    private String leaseKey;
    @Column(name = "owner_id", nullable = false, length = 240)
    private String ownerId;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "heartbeat_at", nullable = false)
    private Instant heartbeatAt;

    protected ControllerLease() { }

    public ControllerLease(String leaseKey, String ownerId, Instant expiresAt, Instant heartbeatAt) {
        this.leaseKey = leaseKey;
        this.ownerId = ownerId;
        this.expiresAt = expiresAt;
        this.heartbeatAt = heartbeatAt;
    }

    public void renew(String ownerId, Instant expiresAt, Instant heartbeatAt) {
        this.ownerId = ownerId;
        this.expiresAt = expiresAt;
        this.heartbeatAt = heartbeatAt;
    }

    public String getLeaseKey() { return leaseKey; }
    public String getOwnerId() { return ownerId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getHeartbeatAt() { return heartbeatAt; }
}
