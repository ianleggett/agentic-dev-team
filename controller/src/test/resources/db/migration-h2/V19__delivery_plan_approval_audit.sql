ALTER TABLE delivery_plans ADD COLUMN approved_by VARCHAR(240);
ALTER TABLE delivery_plans ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE delivery_plans ADD COLUMN approval_reason VARCHAR(10000);
