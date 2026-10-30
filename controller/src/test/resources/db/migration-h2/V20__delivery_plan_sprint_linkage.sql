ALTER TABLE delivery_plan_versions ADD COLUMN directive_revision_id UUID;
ALTER TABLE sprints ADD COLUMN delivery_plan_id UUID;
ALTER TABLE sprints ADD COLUMN delivery_plan_version_id UUID;
ALTER TABLE sprints ADD COLUMN delivery_milestone_key VARCHAR(120);
CREATE INDEX sprints_delivery_plan_idx ON sprints (delivery_plan_id, delivery_plan_version_id);
