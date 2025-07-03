-- V4__create_personnel_tables.sql
CREATE TABLE personnel (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    employee_number VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    department VARCHAR(100),
    position VARCHAR(100),
    hire_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_personnel_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'TERMINATED'))
);

-- Skills table
CREATE TABLE skills (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    category VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_skill_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DEPRECATED'))
);

-- Personnel skills junction table
CREATE TABLE personnel_skills (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    personnel_id UUID NOT NULL REFERENCES personnel(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    proficiency_level VARCHAR(20) NOT NULL DEFAULT 'BEGINNER',
    certified_date DATE,
    expiry_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_personnel_skill UNIQUE (personnel_id, skill_id),
    CONSTRAINT chk_proficiency_level CHECK (proficiency_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT')),
    CONSTRAINT chk_personnel_skill_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED'))
);

-- Indexes
CREATE INDEX idx_personnel_employee_number ON personnel(employee_number);
CREATE INDEX idx_personnel_user_id ON personnel(user_id);
CREATE INDEX idx_personnel_department ON personnel(department);
CREATE INDEX idx_personnel_status ON personnel(status);
CREATE INDEX idx_skills_name ON skills(name);
CREATE INDEX idx_skills_category ON skills(category);
CREATE INDEX idx_personnel_skills_personnel ON personnel_skills(personnel_id);
CREATE INDEX idx_personnel_skills_skill ON personnel_skills(skill_id);

-- Triggers
CREATE TRIGGER update_personnel_updated_at
    BEFORE UPDATE ON personnel
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_skills_updated_at
    BEFORE UPDATE ON skills
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_personnel_skills_updated_at
    BEFORE UPDATE ON personnel_skills
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();