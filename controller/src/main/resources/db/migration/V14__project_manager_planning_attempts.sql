CREATE TABLE project_manager_planning_attempts (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    state VARCHAR(40) NOT NULL,
    prompt TEXT NOT NULL,
    model_output TEXT,
    provider_identifier VARCHAR(255),
    model_identifier VARCHAR(255),
    base_url VARCHAR(1000),
    error_code VARCHAR(255),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_pm_planning_attempts_project_created ON project_manager_planning_attempts(project_id, created_at DESC);
