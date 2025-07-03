-- V5__create_equipment_tables.sql
CREATE TABLE equipment (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    equipment_number VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    serial_number VARCHAR(100),
    location VARCHAR(200),
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_equipment_status CHECK (status IN ('AVAILABLE', 'IN_USE', 'MAINTENANCE', 'OUT_OF_ORDER', 'RETIRED'))
);

-- Maintenance schedules table
CREATE TABLE maintenance_schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    equipment_id UUID NOT NULL REFERENCES equipment(id) ON DELETE CASCADE,
    maintenance_type VARCHAR(50) NOT NULL,
    frequency_days INTEGER NOT NULL,
    last_maintenance_date DATE,
    next_maintenance_date DATE NOT NULL,
    assigned_to UUID REFERENCES users(id),
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_maintenance_status CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'OVERDUE', 'CANCELLED'))
);

-- Indexes
CREATE INDEX idx_equipment_number ON equipment(equipment_number);
CREATE INDEX idx_equipment_status ON equipment(status);
CREATE INDEX idx_equipment_location ON equipment(location);
CREATE INDEX idx_maintenance_equipment ON maintenance_schedules(equipment_id);
CREATE INDEX idx_maintenance_next_date ON maintenance_schedules(next_maintenance_date);
CREATE INDEX idx_maintenance_status ON maintenance_schedules(status);

-- Triggers
CREATE TRIGGER update_equipment_updated_at
    BEFORE UPDATE ON equipment
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_maintenance_schedules_updated_at
    BEFORE UPDATE ON maintenance_schedules
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();