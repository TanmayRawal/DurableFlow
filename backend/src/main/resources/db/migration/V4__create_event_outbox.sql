CREATE TABLE event_outbox (
    id UUID PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    payload_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    publish_attempts INTEGER NOT NULL DEFAULT 0,
    record_version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_event_outbox_pending ON event_outbox (created_at) WHERE published_at IS NULL;
