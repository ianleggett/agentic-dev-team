ALTER TABLE delivery_plans
    ADD COLUMN approved_by VARCHAR(240),
    ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN approval_reason TEXT;
