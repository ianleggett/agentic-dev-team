CREATE TABLE users (
    id UUID PRIMARY KEY,
    subject VARCHAR(240) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS default_branch VARCHAR(120) NOT NULL DEFAULT 'main',
    ADD COLUMN IF NOT EXISTS execution_mode VARCHAR(32) NOT NULL DEFAULT 'GLOBAL_SERIAL',
    ADD COLUMN IF NOT EXISTS max_concurrent_agent_runs INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS max_concurrent_runs_per_project INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS approval_required BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE projects
    ADD CONSTRAINT projects_execution_mode_ck
        CHECK (execution_mode IN ('GLOBAL_SERIAL', 'LOCKED_CONCURRENT')),
    ADD CONSTRAINT projects_concurrency_ck
        CHECK (max_concurrent_agent_runs > 0 AND max_concurrent_runs_per_project > 0);

CREATE TABLE repositories (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
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
    profile_id UUID NOT NULL REFERENCES agent_profiles(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    model_identifier VARCHAR(160) NOT NULL,
    provider_identifier VARCHAR(120) NOT NULL,
    prompt_reference VARCHAR(240) NOT NULL,
    tool_policy TEXT NOT NULL,
    permission_policy TEXT NOT NULL,
    environment_policy TEXT NOT NULL,
    max_steps INTEGER NOT NULL,
    timeout_seconds INTEGER NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT agent_profile_versions_uq UNIQUE (profile_id, version),
    CONSTRAINT agent_profile_versions_limits_ck CHECK (max_steps > 0 AND timeout_seconds > 0)
);

CREATE TABLE directive_revisions (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    content TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT directive_revisions_uq UNIQUE (project_id, version)
);

CREATE TABLE project_managers (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL UNIQUE REFERENCES projects(id) ON DELETE CASCADE,
    agent_profile_version_id UUID NOT NULL REFERENCES agent_profile_versions(id),
    temporal_workflow_id VARCHAR(240) UNIQUE,
    state VARCHAR(40) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT project_managers_state_ck CHECK (state IN (
        'DISABLED', 'WAITING_FOR_LEASE', 'PLANNING', 'WAITING_FOR_APPROVAL',
        'WAITING_FOR_SPRINT', 'REPLANNING', 'WAITING_FOR_EVENT', 'FAILED'
    ))
);

CREATE TABLE sprints (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    objective VARCHAR(500) NOT NULL,
    state VARCHAR(40) NOT NULL,
    current_plan_version INTEGER NOT NULL DEFAULT 1,
    temporal_workflow_id VARCHAR(240) UNIQUE,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT sprints_state_ck CHECK (state IN (
        'DRAFT', 'PLANNED', 'WAITING_FOR_APPROVAL', 'READY', 'RUNNING',
        'PAUSED', 'FAILED', 'COMPLETED', 'CANCELLED'
    ))
);

CREATE TABLE sprint_plan_versions (
    id UUID PRIMARY KEY,
    sprint_id UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    directive_revision_id UUID NOT NULL REFERENCES directive_revisions(id),
    reasoning TEXT NOT NULL,
    approval_policy TEXT NOT NULL,
    failure_policy TEXT NOT NULL,
    created_by VARCHAR(80) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT sprint_plan_versions_uq UNIQUE (sprint_id, version)
);

CREATE TABLE sprint_steps (
    id UUID PRIMARY KEY,
    plan_version_id UUID NOT NULL REFERENCES sprint_plan_versions(id) ON DELETE CASCADE,
    step_key VARCHAR(120) NOT NULL,
    bead_ids TEXT NOT NULL,
    agent_role VARCHAR(60) NOT NULL,
    agent_profile_version_id UUID NOT NULL REFERENCES agent_profile_versions(id),
    repository_ids TEXT NOT NULL,
    state VARCHAR(40) NOT NULL,
    success_criteria TEXT NOT NULL,
    failure_policy TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT sprint_steps_plan_key_uq UNIQUE (plan_version_id, step_key),
    CONSTRAINT sprint_steps_state_ck CHECK (state IN (
        'PENDING', 'READY', 'RUNNING', 'SUCCEEDED', 'FAILED', 'BLOCKED',
        'WAITING_FOR_HUMAN', 'SKIPPED', 'CANCELLED'
    ))
);

CREATE TABLE step_dependencies (
    step_id UUID NOT NULL REFERENCES sprint_steps(id) ON DELETE CASCADE,
    depends_on_step_id UUID NOT NULL REFERENCES sprint_steps(id) ON DELETE CASCADE,
    PRIMARY KEY (step_id, depends_on_step_id),
    CONSTRAINT step_dependencies_self_ck CHECK (step_id <> depends_on_step_id)
);

CREATE TABLE leases (
    id UUID PRIMARY KEY,
    lease_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(240) NOT NULL,
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    sprint_id UUID REFERENCES sprints(id) ON DELETE CASCADE,
    step_id UUID REFERENCES sprint_steps(id) ON DELETE CASCADE,
    heartbeat_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT leases_type_ck CHECK (lease_type IN ('PROJECT_MANAGER', 'PLANNING_TURN', 'AGENT_RUN')),
    CONSTRAINT leases_expiry_ck CHECK (expires_at > created_at)
);

CREATE TABLE resource_locks (
    id UUID PRIMARY KEY,
    lock_key VARCHAR(240) NOT NULL,
    lock_mode VARCHAR(20) NOT NULL DEFAULT 'EXCLUSIVE',
    owner_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(240) NOT NULL,
    lease_id UUID NOT NULL REFERENCES leases(id) ON DELETE CASCADE,
    acquired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    released_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT resource_locks_mode_ck CHECK (lock_mode = 'EXCLUSIVE'),
    CONSTRAINT resource_locks_owner_ck CHECK (owner_type IN ('PLANNING_TURN', 'SPRINT_STEP', 'AGENT_RUN'))
);

CREATE UNIQUE INDEX resource_locks_active_key_uq
    ON resource_locks (lock_key) WHERE released_at IS NULL;

CREATE TABLE agent_runs (
    id UUID PRIMARY KEY,
    step_id UUID NOT NULL REFERENCES sprint_steps(id) ON DELETE CASCADE,
    attempt_number INTEGER NOT NULL,
    idempotency_key VARCHAR(240) NOT NULL UNIQUE,
    state VARCHAR(40) NOT NULL,
    executor_id VARCHAR(240),
    workspace_id VARCHAR(240) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    heartbeat_at TIMESTAMP WITH TIME ZONE,
    finished_at TIMESTAMP WITH TIME ZONE,
    error_code VARCHAR(80),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT agent_runs_step_attempt_uq UNIQUE (step_id, attempt_number),
    CONSTRAINT agent_runs_state_ck CHECK (state IN (
        'CREATED', 'STARTING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'TIMED_OUT',
        'CANCEL_REQUESTED', 'CANCELLED'
    ))
);

CREATE TABLE run_events (
    id UUID PRIMARY KEY,
    agent_run_id UUID NOT NULL REFERENCES agent_runs(id) ON DELETE CASCADE,
    sequence_number BIGINT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload TEXT NOT NULL,
    redacted BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT run_events_sequence_uq UNIQUE (agent_run_id, sequence_number)
);

CREATE TABLE approvals (
    id UUID PRIMARY KEY,
    sprint_id UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
    plan_version_id UUID NOT NULL REFERENCES sprint_plan_versions(id),
    state VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE,
    decided_at TIMESTAMP WITH TIME ZONE,
    decided_by UUID REFERENCES users(id),
    reason TEXT,
    CONSTRAINT approvals_state_ck CHECK (state IN ('NOT_REQUIRED', 'PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE artifact_references (
    id UUID PRIMARY KEY,
    agent_run_id UUID REFERENCES agent_runs(id) ON DELETE CASCADE,
    sprint_id UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
    artifact_type VARCHAR(60) NOT NULL,
    uri VARCHAR(1000) NOT NULL,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    actor_type VARCHAR(20) NOT NULL,
    actor_id VARCHAR(240) NOT NULL,
    project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    sprint_id UUID REFERENCES sprints(id) ON DELETE SET NULL,
    step_id UUID REFERENCES sprint_steps(id) ON DELETE SET NULL,
    event_type VARCHAR(100) NOT NULL,
    from_state VARCHAR(60),
    to_state VARCHAR(60),
    reason TEXT,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT audit_events_actor_ck CHECK (actor_type IN ('USER', 'PROJECT_MANAGER', 'AGENT', 'SYSTEM'))
);

CREATE INDEX repositories_project_active_idx ON repositories (project_id, active);
CREATE INDEX directive_revisions_project_active_idx ON directive_revisions (project_id, active);
CREATE INDEX sprints_project_state_idx ON sprints (project_id, state);
CREATE INDEX sprint_steps_plan_state_idx ON sprint_steps (plan_version_id, state);
CREATE INDEX leases_expiry_idx ON leases (expires_at);
CREATE INDEX leases_owner_idx ON leases (owner_id);
CREATE INDEX agent_runs_step_state_idx ON agent_runs (step_id, state);
CREATE INDEX audit_events_project_created_idx ON audit_events (project_id, created_at);
