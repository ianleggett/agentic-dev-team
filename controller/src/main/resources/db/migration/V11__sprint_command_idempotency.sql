CREATE TABLE sprint_command_idempotency (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(240) NOT NULL UNIQUE,
    sprint_id UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
    action VARCHAR(40) NOT NULL,
    plan_version INTEGER NOT NULL,
    reason TEXT NOT NULL,
    result_state VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
