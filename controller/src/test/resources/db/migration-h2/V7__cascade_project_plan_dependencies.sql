ALTER TABLE sprint_plan_versions DROP CONSTRAINT IF EXISTS sprint_plan_versions_directive_revision_id_fkey;
ALTER TABLE sprint_plan_versions ADD CONSTRAINT sprint_plan_versions_directive_revision_id_fkey
    FOREIGN KEY (directive_revision_id) REFERENCES directive_revisions(id) ON DELETE CASCADE;
ALTER TABLE approvals DROP CONSTRAINT IF EXISTS approvals_plan_version_id_fkey;
ALTER TABLE approvals ADD CONSTRAINT approvals_plan_version_id_fkey
    FOREIGN KEY (plan_version_id) REFERENCES sprint_plan_versions(id) ON DELETE CASCADE;
