-- src/main/resources/db/migration/V1__create_base_tables.sql

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create base audit columns function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create sequence for numeric IDs if needed
CREATE SEQUENCE IF NOT EXISTS global_id_seq START 1000;