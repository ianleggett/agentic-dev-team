UPDATE agent_profile_versions
SET base_url = 'http://gx10-c16d.internal:8888/v1'
WHERE base_url = 'http://host.docker.internal:8000/v1';
