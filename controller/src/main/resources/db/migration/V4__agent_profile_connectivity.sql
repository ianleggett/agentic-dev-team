ALTER TABLE agent_profile_versions
    ADD COLUMN IF NOT EXISTS base_url VARCHAR(500) NOT NULL DEFAULT 'http://host.docker.internal:8000/v1',
    ADD COLUMN IF NOT EXISTS api_key TEXT,
    ADD COLUMN IF NOT EXISTS team_prompt TEXT NOT NULL DEFAULT '';
