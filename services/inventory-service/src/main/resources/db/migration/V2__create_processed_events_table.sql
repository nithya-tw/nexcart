-- Create processed_events table for idempotent event processing

CREATE TABLE processed_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_processed_event_id UNIQUE (event_id)
);

CREATE INDEX idx_processed_event_id ON processed_events(event_id);
CREATE INDEX idx_processed_event_type ON processed_events(event_type, processed_at);

COMMENT ON TABLE processed_events IS 'Tracks processed events for idempotent consumer';
COMMENT ON COLUMN processed_events.event_id IS 'Unique event identifier from Kafka';
COMMENT ON COLUMN processed_events.event_type IS 'Type of event processed (e.g., OrderPlaced)';
