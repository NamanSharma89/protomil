-- V3__create_jobcard_tables.sql
CREATE TABLE job_cards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_number VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    assigned_to UUID REFERENCES users(id),
    equipment_id UUID,
    estimated_hours DECIMAL(10,2),
    actual_hours DECIMAL(10,2),
    due_date TIMESTAMP,
    completed_at TIMESTAMP,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,

    -- Constraints
    CONSTRAINT chk_job_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'ON_HOLD')),
    CONSTRAINT chk_job_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- Indexes
CREATE INDEX idx_job_cards_number ON job_cards(job_number);
CREATE INDEX idx_job_cards_status ON job_cards(status);
CREATE INDEX idx_job_cards_assigned_to ON job_cards(assigned_to);
CREATE INDEX idx_job_cards_due_date ON job_cards(due_date);
CREATE INDEX idx_job_cards_created_at ON job_cards(created_at);

-- Trigger for updated_at
CREATE TRIGGER update_job_cards_updated_at
    BEFORE UPDATE ON job_cards
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Job instructions table
CREATE TABLE job_instructions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_card_id UUID NOT NULL REFERENCES job_cards(id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    instruction TEXT NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP,
    completed_by UUID REFERENCES users(id),

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_job_instruction_step UNIQUE (job_card_id, step_number)
);

-- Indexes
CREATE INDEX idx_job_instructions_job_card ON job_instructions(job_card_id);
CREATE INDEX idx_job_instructions_step ON job_instructions(step_number);

-- Trigger for updated_at
CREATE TRIGGER update_job_instructions_updated_at
    BEFORE UPDATE ON job_instructions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();