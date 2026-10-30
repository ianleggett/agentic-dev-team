ALTER TABLE sprints ADD COLUMN temporal_run_id VARCHAR(240);
ALTER TABLE sprints ADD COLUMN state_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sprints ADD COLUMN recovery_reason VARCHAR(10000);

ALTER TABLE agent_runs ADD COLUMN temporal_workflow_id VARCHAR(240);
ALTER TABLE agent_runs ADD COLUMN temporal_run_id VARCHAR(240);
ALTER TABLE agent_runs ADD COLUMN state_version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX agent_runs_temporal_identity_idx
    ON agent_runs (temporal_workflow_id, temporal_run_id);
