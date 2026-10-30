CREATE TABLE delivery_plans (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    objective VARCHAR(500) NOT NULL,
    state VARCHAR(40) NOT NULL,
    current_version INTEGER NOT NULL DEFAULT 1,
    approved_version INTEGER,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE delivery_plan_versions (
    id UUID PRIMARY KEY,
    delivery_plan_id UUID NOT NULL,
    version INTEGER NOT NULL,
    objective VARCHAR(500) NOT NULL,
    reasoning VARCHAR(10000) NOT NULL,
    acceptance_criteria VARCHAR(10000) NOT NULL,
    suggested_sprint_boundaries VARCHAR(10000) NOT NULL,
    plan_snapshot VARCHAR(10000) NOT NULL,
    created_by VARCHAR(80) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT delivery_plan_versions_uq UNIQUE (delivery_plan_id, version)
);

CREATE TABLE delivery_milestones (
    id UUID PRIMARY KEY,
    delivery_plan_version_id UUID NOT NULL,
    milestone_key VARCHAR(120) NOT NULL,
    title VARCHAR(240) NOT NULL,
    outcome VARCHAR(10000) NOT NULL,
    sequence_number INTEGER NOT NULL,
    state VARCHAR(40) NOT NULL DEFAULT 'PLANNED',
    CONSTRAINT delivery_milestones_key_uq UNIQUE (delivery_plan_version_id, milestone_key)
);

CREATE TABLE delivery_plan_beads (
    id UUID PRIMARY KEY,
    delivery_plan_version_id UUID NOT NULL,
    bead_id VARCHAR(240) NOT NULL,
    milestone_key VARCHAR(120),
    title_snapshot VARCHAR(500) NOT NULL,
    status_snapshot VARCHAR(80) NOT NULL,
    dependency_ids_snapshot VARCHAR(10000) NOT NULL,
    labels_snapshot VARCHAR(10000) NOT NULL,
    bead_snapshot VARCHAR(10000) NOT NULL,
    CONSTRAINT delivery_plan_beads_uq UNIQUE (delivery_plan_version_id, bead_id)
);

CREATE INDEX delivery_plans_project_state_idx ON delivery_plans (project_id, state);
CREATE INDEX delivery_plan_versions_plan_idx ON delivery_plan_versions (delivery_plan_id, version);
CREATE INDEX delivery_plan_beads_bead_idx ON delivery_plan_beads (bead_id);
