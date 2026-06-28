-- Create outbox_events table for Transactional Outbox Pattern

CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processed_at TIMESTAMP,
    CONSTRAINT uk_outbox_event_id UNIQUE (event_id)
);

CREATE INDEX idx_outbox_processed ON outbox_events(processed, created_at);
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);

COMMENT ON TABLE outbox_events IS 'Transactional outbox for reliable event publishing';
COMMENT ON COLUMN outbox_events.event_id IS 'Unique event identifier (UUID)';
COMMENT ON COLUMN outbox_events.event_type IS 'Type of domain event (e.g., OrderPlaced)';
COMMENT ON COLUMN outbox_events.aggregate_type IS 'Type of aggregate (e.g., Order)';
COMMENT ON COLUMN outbox_events.aggregate_id IS 'ID of the aggregate that generated the event';
COMMENT ON COLUMN outbox_events.payload IS 'JSON payload of the event';
COMMENT ON COLUMN outbox_events.processed IS 'Whether event has been published to Kafka';
