CREATE TABLE project_manager_planning_attempts (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    state VARCHAR(40) NOT NULL,
    prompt VARCHAR(1000000) NOT NULL,
    model_output VARCHAR(1000000),
    provider_identifier VARCHAR(255),
    model_identifier VARCHAR(255),
    base_url VARCHAR(1000),
    error_code VARCHAR(255),
    error_message VARCHAR(1000000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE
);
