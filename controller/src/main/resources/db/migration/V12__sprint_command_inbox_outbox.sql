CREATE TABLE sprint_commands (
    id UUID PRIMARY KEY,
    sprint_id UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(240) NOT NULL,
    action VARCHAR(40) NOT NULL,
    plan_version INTEGER NOT NULL,
    reason TEXT NOT NULL,
    payload TEXT NOT NULL,
    actor_subject VARCHAR(240) NOT NULL,
    actor_name VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL,
    result_state VARCHAR(40),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    applied_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT sprint_commands_key_uq UNIQUE (sprint_id, idempotency_key)
);
CREATE TABLE sprint_command_outbox (
    command_id UUID PRIMARY KEY REFERENCES sprint_commands(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_error TEXT
);
