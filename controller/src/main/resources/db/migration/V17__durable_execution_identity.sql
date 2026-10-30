ALTER TABLE sprints
    ADD COLUMN IF NOT EXISTS temporal_run_id VARCHAR(240),
    ADD COLUMN IF NOT EXISTS state_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS recovery_reason TEXT;

ALTER TABLE agent_runs
    ADD COLUMN IF NOT EXISTS temporal_workflow_id VARCHAR(240),
    ADD COLUMN IF NOT EXISTS temporal_run_id VARCHAR(240),
    ADD COLUMN IF NOT EXISTS state_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE agent_runs DROP CONSTRAINT IF EXISTS agent_runs_state_ck;
ALTER TABLE agent_runs ADD CONSTRAINT agent_runs_state_ck CHECK (state IN (
    'CREATED', 'STARTING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'TIMED_OUT',
    'CANCEL_REQUESTED', 'CANCELLED', 'UNKNOWN', 'RECOVERY_REQUIRED'
));

ALTER TABLE sprints DROP CONSTRAINT IF EXISTS sprints_state_ck;
ALTER TABLE sprints ADD CONSTRAINT sprints_state_ck CHECK (state IN (
    'DRAFT', 'PLANNED', 'WAITING_FOR_APPROVAL', 'READY', 'RUNNING',
    'PAUSED', 'FAILED', 'COMPLETED', 'CANCELLED', 'UNKNOWN', 'RECOVERY_REQUIRED'
));

CREATE INDEX IF NOT EXISTS agent_runs_temporal_identity_idx
    ON agent_runs (temporal_workflow_id, temporal_run_id);
