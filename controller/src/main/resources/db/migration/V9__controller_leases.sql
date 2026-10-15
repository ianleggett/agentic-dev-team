CREATE TABLE controller_leases (
    lease_key VARCHAR(240) PRIMARY KEY,
    owner_id VARCHAR(240) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    heartbeat_at TIMESTAMP WITH TIME ZONE NOT NULL
);
