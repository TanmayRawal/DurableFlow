ALTER TABLE task_executions
    ADD COLUMN handler_config_json JSONB NOT NULL DEFAULT '{}'::jsonb;
