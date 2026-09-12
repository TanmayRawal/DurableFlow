ALTER TABLE workflow_runs ADD COLUMN idempotency_key VARCHAR(160);
ALTER TABLE workflow_runs ADD CONSTRAINT uq_run_definition_idempotency UNIQUE (workflow_definition_id, idempotency_key);

ALTER TABLE task_executions ADD COLUMN max_attempts INTEGER NOT NULL DEFAULT 3;
ALTER TABLE task_executions ADD COLUMN next_attempt_at TIMESTAMPTZ;
ALTER TABLE task_executions ADD COLUMN lease_owner VARCHAR(160);
ALTER TABLE task_executions ADD COLUMN lease_expires_at TIMESTAMPTZ;
ALTER TABLE task_executions ADD COLUMN last_heartbeat_at TIMESTAMPTZ;
ALTER TABLE task_executions ADD COLUMN failure_reason VARCHAR(1000);
ALTER TABLE task_executions DROP CONSTRAINT chk_task_status;
ALTER TABLE task_executions ADD CONSTRAINT chk_task_status CHECK (status IN ('PENDING', 'READY', 'RUNNING', 'RETRYING', 'SUCCEEDED', 'FAILED', 'DEAD_LETTER', 'CANCELLED'));
CREATE INDEX idx_task_retry_due ON task_executions (next_attempt_at) WHERE status = 'RETRYING';
CREATE INDEX idx_task_lease_expiry ON task_executions (lease_expires_at) WHERE status = 'RUNNING';
