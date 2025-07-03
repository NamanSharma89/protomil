-- src/main/resources/db/migration/V8__create_spring_modulith_tables.sql

-- Spring Modulith Event Publication table
-- This table is used by Spring Modulith to track published events
CREATE TABLE event_publication (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP NOT NULL,
    completion_date TIMESTAMP
);

-- Create indexes separately (PostgreSQL syntax)
CREATE INDEX idx_event_publication_date ON event_publication(publication_date);
CREATE INDEX idx_event_publication_completion ON event_publication(completion_date);
CREATE INDEX idx_event_publication_listener ON event_publication(listener_id);
CREATE INDEX idx_event_publication_type ON event_publication(event_type);

-- Create partial index on incomplete events for cleanup queries
CREATE INDEX idx_event_publication_incomplete ON event_publication(completion_date) WHERE completion_date IS NULL;