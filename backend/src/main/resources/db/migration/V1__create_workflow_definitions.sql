CREATE TABLE workflow_definitions (
    id UUID PRIMARY KEY,
    workflow_name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    version INTEGER NOT NULL,
    definition_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_workflow_name_version UNIQUE (workflow_name, version)
);

CREATE INDEX idx_workflow_definitions_name ON workflow_definitions (workflow_name);
