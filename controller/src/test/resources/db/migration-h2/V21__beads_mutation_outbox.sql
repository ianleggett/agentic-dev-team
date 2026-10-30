CREATE TABLE beads_mutation_outbox (
    id UUID PRIMARY KEY,
    sprint_id UUID NOT NULL,
    mutation_key VARCHAR(240) NOT NULL UNIQUE,
    bead_id VARCHAR(120) NOT NULL,
    operation VARCHAR(40) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error TEXT
);
