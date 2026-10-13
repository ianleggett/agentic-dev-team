CREATE TABLE users (
    id UUID PRIMARY KEY,
    subject VARCHAR(240) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE projects
    ADD default_branch VARCHAR(120) NOT NULL DEFAULT 'main';

ALTER TABLE projects
    ADD execution_mode VARCHAR(32) NOT NULL DEFAULT 'GLOBAL_SERIAL';

ALTER TABLE projects
    ADD max_concurrent_agent_runs INTEGER NOT NULL DEFAULT 1;

ALTER TABLE projects
    ADD max_concurrent_runs_per_project INTEGER NOT NULL DEFAULT 1;

ALTER TABLE projects
    ADD approval_required BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE repositories (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    remote_url VARCHAR(500) NOT NULL,
    default_branch VARCHAR(120) NOT NULL,
    role VARCHAR(40) NOT NULL,
    relative_workspace_path VARCHAR(240) NOT NULL,
    build_commands TEXT NOT NULL,
    test_commands TEXT NOT NULL,
    health_check TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT repositories_project_name_uq UNIQUE (project_id, name),
    CONSTRAINT repositories_project_workspace_uq UNIQUE (project_id, relative_workspace_path)
);

CREATE TABLE agent_profiles (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    role VARCHAR(60) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE agent_profile_versions (
    id UUID PRIMARY KEY,
    profile_id UUID NOT NULL,
    version INTEGER NOT NULL,
    model_identifier VARCHAR(160) NOT NULL,
    provider_identifier VARCHAR(120) NOT NULL,
    prompt_reference VARCHAR(240) NOT NULL,
    tool_policy VARCHAR(4000) NOT NULL,
    permission_policy VARCHAR(4000) NOT NULL,
    environment_policy VARCHAR(4000) NOT NULL,
    max_steps INTEGER NOT NULL,
    timeout_seconds INTEGER NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT agent_profile_versions_uq UNIQUE (profile_id, version)
);

CREATE TABLE directive_revisions (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    version INTEGER NOT NULL,
    content VARCHAR(10000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT directive_revisions_uq UNIQUE (project_id, version)
);

CREATE TABLE project_managers (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL UNIQUE,
    agent_profile_version_id UUID NOT NULL,
    temporal_workflow_id VARCHAR(240) UNIQUE,
    state VARCHAR(40) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE sprints (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    objective VARCHAR(500) NOT NULL,
    state VARCHAR(40) NOT NULL,
    current_plan_version INTEGER NOT NULL DEFAULT 1,
    temporal_workflow_id VARCHAR(240) UNIQUE,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE sprint_plan_versions (
    id UUID PRIMARY KEY,
    sprint_id UUID NOT NULL,
    version INTEGER NOT NULL,
    directive_revision_id UUID NOT NULL,
    reasoning VARCHAR(10000) NOT NULL,
    approval_policy VARCHAR(4000) NOT NULL,
    failure_policy VARCHAR(4000) NOT NULL,
    created_by VARCHAR(80) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT sprint_plan_versions_uq UNIQUE (sprint_id, version)
);

CREATE TABLE sprint_steps (
    id UUID PRIMARY KEY,
    plan_version_id UUID NOT NULL,
    step_key VARCHAR(120) NOT NULL,
    bead_ids VARCHAR(4000) NOT NULL,
    agent_role VARCHAR(60) NOT NULL,
    agent_profile_version_id UUID NOT NULL,
    repository_ids VARCHAR(4000) NOT NULL,
    state VARCHAR(40) NOT NULL,
    success_criteria VARCHAR(4000) NOT NULL,
    failure_policy VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT sprint_steps_plan_key_uq UNIQUE (plan_version_id, step_key)
);

CREATE TABLE step_dependencies (
    step_id UUID NOT NULL,
    depends_on_step_id UUID NOT NULL,
    PRIMARY KEY (step_id, depends_on_step_id)
);

CREATE TABLE leases (
    id UUID PRIMARY KEY,
    lease_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(240) NOT NULL,
    project_id UUID,
    sprint_id UUID,
    step_id UUID,
    heartbeat_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE resource_locks (
    id UUID PRIMARY KEY,
    lock_key VARCHAR(240) NOT NULL,
    lock_mode VARCHAR(20) NOT NULL DEFAULT 'EXCLUSIVE',
    owner_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(240) NOT NULL,
    lease_id UUID NOT NULL,
    acquired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    released_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX resource_locks_active_key_uq
    ON resource_locks (lock_key, released_at);

CREATE TABLE agent_runs (
    id UUID PRIMARY KEY,
    step_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL,
    idempotency_key VARCHAR(240) NOT NULL UNIQUE,
    state VARCHAR(40) NOT NULL,
    executor_id VARCHAR(240),
    workspace_id VARCHAR(240) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    heartbeat_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    error_code VARCHAR(80),
    error_message VARCHAR(10000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT agent_runs_step_attempt_uq UNIQUE (step_id, attempt_number)
);

CREATE TABLE run_events (
    id UUID PRIMARY KEY,
    agent_run_id UUID NOT NULL,
    sequence_number BIGINT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload VARCHAR(10000) NOT NULL,
    redacted BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT run_events_sequence_uq UNIQUE (agent_run_id, sequence_number)
);

CREATE TABLE approvals (
    id UUID PRIMARY KEY,
    sprint_id UUID NOT NULL,
    plan_version_id UUID NOT NULL,
    state VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE,
    decided_at TIMESTAMP WITH TIME ZONE,
    decided_by UUID,
    reason VARCHAR(10000)
);

CREATE TABLE artifact_references (
    id UUID PRIMARY KEY,
    agent_run_id UUID,
    sprint_id UUID NOT NULL,
    artifact_type VARCHAR(60) NOT NULL,
    uri VARCHAR(1000) NOT NULL,
    metadata VARCHAR(10000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor_type VARCHAR(20) NOT NULL,
    actor_id VARCHAR(240) NOT NULL,
    project_id UUID,
    sprint_id UUID,
    step_id UUID,
    event_type VARCHAR(100) NOT NULL,
    from_state VARCHAR(60),
    to_state VARCHAR(60),
    reason VARCHAR(10000),
    payload VARCHAR(10000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
