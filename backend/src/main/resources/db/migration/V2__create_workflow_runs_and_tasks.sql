CREATE TABLE workflow_runs (
    id UUID PRIMARY KEY,
    workflow_definition_id UUID NOT NULL REFERENCES workflow_definitions(id),
    status VARCHAR(32) NOT NULL,
    input_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    record_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_workflow_run_status CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED'))
);

CREATE TABLE task_executions (
    id UUID PRIMARY KEY,
    workflow_run_id UUID NOT NULL REFERENCES workflow_runs(id) ON DELETE CASCADE,
    node_key VARCHAR(160) NOT NULL,
    handler_type VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    record_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_run_node UNIQUE (workflow_run_id, node_key),
    CONSTRAINT chk_task_status CHECK (status IN ('PENDING', 'READY', 'RUNNING', 'SUCCEEDED', 'FAILED', 'DEAD_LETTER', 'CANCELLED'))
);

CREATE INDEX idx_task_executions_ready ON task_executions (status, created_at) WHERE status = 'READY';
CREATE INDEX idx_task_executions_run ON task_executions (workflow_run_id);
