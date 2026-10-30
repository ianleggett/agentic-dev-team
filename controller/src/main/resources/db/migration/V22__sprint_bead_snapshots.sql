CREATE TABLE sprint_bead_snapshots (
    id UUID PRIMARY KEY,
    sprint_id UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
    bead_id VARCHAR(240) NOT NULL,
    step_key VARCHAR(120) NOT NULL,
    title_snapshot VARCHAR(500) NOT NULL,
    status_snapshot VARCHAR(80) NOT NULL,
    dependency_ids_snapshot TEXT NOT NULL,
    labels_snapshot TEXT NOT NULL,
    bead_snapshot TEXT NOT NULL,
    selection_reason TEXT NOT NULL,
    CONSTRAINT sprint_bead_snapshots_uq UNIQUE (sprint_id, bead_id)
);

CREATE INDEX sprint_bead_snapshots_sprint_idx ON sprint_bead_snapshots (sprint_id);
