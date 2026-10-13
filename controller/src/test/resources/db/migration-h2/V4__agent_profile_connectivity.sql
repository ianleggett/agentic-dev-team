ALTER TABLE agent_profile_versions ADD COLUMN IF NOT EXISTS base_url VARCHAR(500) NOT NULL DEFAULT 'http://host.docker.internal:8000/v1';
ALTER TABLE agent_profile_versions ADD COLUMN IF NOT EXISTS api_key VARCHAR(4000);
ALTER TABLE agent_profile_versions ADD COLUMN IF NOT EXISTS team_prompt VARCHAR(100000) NOT NULL DEFAULT '';
