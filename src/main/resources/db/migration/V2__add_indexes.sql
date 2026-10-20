-- V2: indexes for the work order filters and the History page
CREATE INDEX IF NOT EXISTS idx_work_orders_status   ON work_orders (status);
CREATE INDEX IF NOT EXISTS idx_work_orders_due_date ON work_orders (due_date);
CREATE INDEX IF NOT EXISTS idx_audit_log_event_time ON audit_log (event_time);
