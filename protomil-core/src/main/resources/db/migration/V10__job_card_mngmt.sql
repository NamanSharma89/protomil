-- ==========================================
-- JOB CARD TEMPLATE SYSTEM
-- ==========================================

-- Job Card Templates with metadata
CREATE TABLE job_card_templates (
    id BIGSERIAL PRIMARY KEY,
    template_name VARCHAR(200) NOT NULL,
    template_code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    category VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN DEFAULT true,
    created_by BIGINT NOT NULL, -- Will reference users.id after migration
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Dynamic field definitions for templates
CREATE TABLE template_field_definitions (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES job_card_templates(id) ON DELETE CASCADE,
    field_name VARCHAR(100) NOT NULL,
    field_label VARCHAR(200) NOT NULL,
    field_type VARCHAR(50) NOT NULL, -- TEXT, NUMBER, DATE, DATETIME, DROPDOWN, CHECKBOX, TEXTAREA, FILE
    field_group VARCHAR(100), -- For organizing fields into sections
    display_order INTEGER NOT NULL,
    is_required BOOLEAN DEFAULT false,
    field_config JSONB DEFAULT '{}', -- validation rules, dropdown options, min/max values
    help_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_field_per_template UNIQUE(template_id, field_name),
    CONSTRAINT valid_field_type CHECK (field_type IN ('TEXT', 'NUMBER', 'DATE', 'DATETIME', 'DROPDOWN', 'CHECKBOX', 'TEXTAREA', 'FILE'))
);

-- Pre-defined field options for dropdowns
CREATE TABLE field_options (
    id BIGSERIAL PRIMARY KEY,
    field_definition_id BIGINT NOT NULL REFERENCES template_field_definitions(id) ON DELETE CASCADE,
    option_value VARCHAR(200) NOT NULL,
    option_label VARCHAR(200) NOT NULL,
    display_order INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- JOB CARD CORE TABLES
-- ==========================================

-- Job cards with dynamic structure
CREATE TABLE job_cards (
    id BIGSERIAL PRIMARY KEY,
    job_number VARCHAR(50) UNIQUE NOT NULL,
    template_id BIGINT NOT NULL REFERENCES job_card_templates(id),
    title VARCHAR(200) NOT NULL,
    description TEXT,

    -- Status and workflow
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    priority VARCHAR(20) DEFAULT 'MEDIUM',

    -- Assignment and timing
    created_by BIGINT NOT NULL, -- Will reference users.id
    assigned_to BIGINT, -- Will reference users.id
    estimated_duration_minutes INTEGER,
    actual_duration_minutes INTEGER,
    target_completion_date TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,

    -- Dynamic field data storage
    dynamic_fields JSONB NOT NULL DEFAULT '{}',

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version INTEGER DEFAULT 1,

    CONSTRAINT valid_status CHECK (status IN ('DRAFT', 'READY', 'ASSIGNED', 'IN_PROGRESS', 'PENDING_REVIEW', 'COMPLETED', 'CANCELLED', 'REWORK_REQUIRED')),
    CONSTRAINT valid_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

-- Work instructions for job cards
CREATE TABLE work_instructions (
    id BIGSERIAL PRIMARY KEY,
    job_card_id BIGINT NOT NULL REFERENCES job_cards(id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    instruction_type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    content TEXT,
    attachments JSONB DEFAULT '[]',
    is_quality_checkpoint BOOLEAN DEFAULT false,
    estimated_duration_minutes INTEGER,
    actual_duration_minutes INTEGER,
    completed_at TIMESTAMP,
    completed_by BIGINT, -- Will reference users.id
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_step_per_job UNIQUE(job_card_id, step_number),
    CONSTRAINT valid_instruction_type CHECK (instruction_type IN ('TEXT', 'IMAGE', 'VIDEO', 'CHECKLIST', 'FORM', 'ATTACHMENT'))
);

-- Job card attachments (files, images, videos)
CREATE TABLE job_card_attachments (
    id BIGSERIAL PRIMARY KEY,
    job_card_id BIGINT NOT NULL REFERENCES job_cards(id) ON DELETE CASCADE,
    work_instruction_id BIGINT REFERENCES work_instructions(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100),
    description TEXT,
    uploaded_by BIGINT NOT NULL, -- Will reference users.id
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_file_size CHECK (file_size_bytes > 0 AND file_size_bytes <= 104857600) -- 100MB max
);

-- ==========================================
-- MASTER DATA TABLES
-- ==========================================

-- Machine Master
CREATE TABLE machines (
    id BIGSERIAL PRIMARY KEY,
    machine_code VARCHAR(50) UNIQUE NOT NULL,
    machine_name VARCHAR(200) NOT NULL,
    section_code VARCHAR(50),
    section_name VARCHAR(200),
    machine_type VARCHAR(100),
    capacity_per_hour INTEGER,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_machine_status CHECK (status IN ('ACTIVE', 'MAINTENANCE', 'BREAKDOWN', 'INACTIVE'))
);

-- Part Master
CREATE TABLE parts (
    id BIGSERIAL PRIMARY KEY,
    part_number VARCHAR(100) UNIQUE NOT NULL,
    part_name VARCHAR(200) NOT NULL,
    part_description TEXT,
    standard_cycle_time_minutes DECIMAL(10,2),
    quality_standards JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Operation Master
CREATE TABLE operations (
    id BIGSERIAL PRIMARY KEY,
    operation_code VARCHAR(50) UNIQUE NOT NULL,
    operation_name VARCHAR(200) NOT NULL,
    operation_description TEXT,
    standard_time_minutes DECIMAL(10,2),
    skill_level_required INTEGER DEFAULT 1,
    quality_checkpoints JSONB DEFAULT '[]',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_skill_level CHECK (skill_level_required BETWEEN 1 AND 4)
);

-- Loss Time Categories (for better reporting)
CREATE TABLE loss_time_categories (
    id BIGSERIAL PRIMARY KEY,
    category_code VARCHAR(20) UNIQUE NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    description TEXT,
    color_code VARCHAR(7) DEFAULT '#808080', -- For UI display
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- PRODUCTION MASTER TABLE
-- ==========================================

-- Production Master Data (based on Excel fields)
CREATE TABLE production_master (
    id BIGSERIAL PRIMARY KEY,
    job_card_id BIGINT NOT NULL REFERENCES job_cards(id),

    -- Entry Information
    entry_date DATE NOT NULL DEFAULT CURRENT_DATE,
    entry_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Machine Information
    machine_code VARCHAR(50),
    machine_name VARCHAR(200),
    section_code VARCHAR(50),

    -- Time Information
    start_date_time TIMESTAMP,
    stop_date_time TIMESTAMP,
    shift VARCHAR(20), -- MORNING, AFTERNOON, NIGHT
    total_time_available_minutes INTEGER,

    -- Loss Time Details (8 separate loss time fields as per Excel)
    loss_time_1_minutes INTEGER DEFAULT 0,
    loss_time_2_minutes INTEGER DEFAULT 0,
    loss_time_3_minutes INTEGER DEFAULT 0,
    loss_time_4_minutes INTEGER DEFAULT 0,
    loss_time_5_minutes INTEGER DEFAULT 0,
    loss_time_6_minutes INTEGER DEFAULT 0,
    loss_time_7_minutes INTEGER DEFAULT 0,
    loss_time_8_minutes INTEGER DEFAULT 0,

    -- Break Information
    lunch_tea_break_minutes INTEGER DEFAULT 0,
    net_time_available_minutes INTEGER GENERATED ALWAYS AS (
        total_time_available_minutes -
        COALESCE(loss_time_1_minutes, 0) - COALESCE(loss_time_2_minutes, 0) -
        COALESCE(loss_time_3_minutes, 0) - COALESCE(loss_time_4_minutes, 0) -
        COALESCE(loss_time_5_minutes, 0) - COALESCE(loss_time_6_minutes, 0) -
        COALESCE(loss_time_7_minutes, 0) - COALESCE(loss_time_8_minutes, 0) -
        COALESCE(lunch_tea_break_minutes, 0)
    ) STORED,

    -- Personnel Information
    employee_number VARCHAR(50),
    operator_name VARCHAR(200),
    team VARCHAR(100),

    -- Part/Operation Information
    part_number VARCHAR(100),
    part_name VARCHAR(200),
    operation_number VARCHAR(50),
    operation_code VARCHAR(50),
    next_operation_number VARCHAR(50),

    -- Operation Timing
    defined_operation_time_minutes DECIMAL(10,2),
    actual_operation_time_minutes DECIMAL(10,2),

    -- Production Quantities
    production_quantity INTEGER DEFAULT 0,
    production_loss_quantity INTEGER DEFAULT 0,
    rejection_inprocess_quantity INTEGER DEFAULT 0,
    rejection_finalstage_quantity INTEGER DEFAULT 0,

    -- Efficiency Metrics (calculated fields)
    operator_efficiency_percentage DECIMAL(5,2) GENERATED ALWAYS AS (
        CASE
            WHEN actual_operation_time_minutes > 0 AND defined_operation_time_minutes > 0
            THEN (defined_operation_time_minutes / actual_operation_time_minutes) * 100
            ELSE NULL
        END
    ) STORED,

    equipment_efficiency_percentage DECIMAL(5,2) GENERATED ALWAYS AS (
        CASE
            WHEN total_time_available_minutes > 0
            THEN (net_time_available_minutes::DECIMAL / total_time_available_minutes) * 100
            ELSE NULL
        END
    ) STORED,

    -- Additional Information
    remarks TEXT,
    breakdown_idle_reason TEXT,

    -- System Fields
    data_source VARCHAR(50) DEFAULT 'JOB_CARD', -- To track data origin
    is_validated BOOLEAN DEFAULT false,
    validated_by BIGINT, -- Will reference users.id
    validated_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_shift CHECK (shift IN ('MORNING', 'AFTERNOON', 'NIGHT')),
    CONSTRAINT valid_quantities CHECK (
        production_quantity >= 0 AND
        production_loss_quantity >= 0 AND
        rejection_inprocess_quantity >= 0 AND
        rejection_finalstage_quantity >= 0
    ),
    CONSTRAINT valid_time_range CHECK (
        start_date_time IS NULL OR stop_date_time IS NULL OR start_date_time <= stop_date_time
    )
);

-- Enhanced loss time tracking with detailed breakdown
CREATE TABLE production_loss_details (
    id BIGSERIAL PRIMARY KEY,
    production_master_id BIGINT NOT NULL REFERENCES production_master(id) ON DELETE CASCADE,
    loss_sequence INTEGER NOT NULL, -- 1-8 corresponding to loss_time_1 to loss_time_8
    loss_category_id BIGINT REFERENCES loss_time_categories(id),
    loss_time_minutes INTEGER NOT NULL,
    loss_reason TEXT,
    corrective_action TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_loss_per_production UNIQUE(production_master_id, loss_sequence),
    CONSTRAINT valid_loss_sequence CHECK (loss_sequence BETWEEN 1 AND 8),
    CONSTRAINT valid_loss_time CHECK (loss_time_minutes >= 0)
);

-- ==========================================
-- ASSIGNMENT AND SKILL TABLES
-- ==========================================

-- Job Card Assignment History
CREATE TABLE job_card_assignments (
    id BIGSERIAL PRIMARY KEY,
    job_card_id BIGINT NOT NULL REFERENCES job_cards(id),
    assigned_to BIGINT NOT NULL, -- Will reference users.id
    assigned_by BIGINT NOT NULL, -- Will reference users.id
    machine_id BIGINT REFERENCES machines(id),
    assignment_reason TEXT,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    unassigned_at TIMESTAMP,
    unassignment_reason TEXT,
    is_active BOOLEAN DEFAULT true
);

-- Equipment assignments for job cards
CREATE TABLE job_card_equipment (
    id BIGSERIAL PRIMARY KEY,
    job_card_id BIGINT NOT NULL REFERENCES job_cards(id),
    machine_id BIGINT NOT NULL REFERENCES machines(id),
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP,
    is_primary BOOLEAN DEFAULT false,

    CONSTRAINT unique_primary_machine UNIQUE(job_card_id, is_primary) DEFERRABLE INITIALLY DEFERRED
);

-- ==========================================
-- NOTIFICATION AND AUDIT TABLES
-- ==========================================

-- Job Card Status History
CREATE TABLE job_card_status_history (
    id BIGSERIAL PRIMARY KEY,
    job_card_id BIGINT NOT NULL REFERENCES job_cards(id),
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    changed_by BIGINT NOT NULL, -- Will reference users.id
    change_reason TEXT,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    additional_data JSONB DEFAULT '{}'
);

-- Job Card Comments/Notes
CREATE TABLE job_card_comments (
    id BIGSERIAL PRIMARY KEY,
    job_card_id BIGINT NOT NULL REFERENCES job_cards(id),
    comment_text TEXT NOT NULL,
    comment_type VARCHAR(50) DEFAULT 'GENERAL',
    created_by BIGINT NOT NULL, -- Will reference users.id
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_internal BOOLEAN DEFAULT true,

    CONSTRAINT valid_comment_type CHECK (comment_type IN ('GENERAL', 'QUALITY', 'ISSUE', 'INSTRUCTION', 'APPROVAL'))
);

-- ==========================================
-- INDEXES FOR PERFORMANCE
-- ==========================================

-- Job Card Indexes
CREATE INDEX idx_job_cards_status ON job_cards(status);
CREATE INDEX idx_job_cards_assigned_to ON job_cards(assigned_to);
CREATE INDEX idx_job_cards_created_by ON job_cards(created_by);
CREATE INDEX idx_job_cards_template ON job_cards(template_id);
CREATE INDEX idx_job_cards_job_number ON job_cards(job_number);
CREATE INDEX idx_job_cards_target_date ON job_cards(target_completion_date);
CREATE INDEX idx_job_cards_created_at ON job_cards(created_at);

-- Production Master Indexes
CREATE INDEX idx_production_master_job_card ON production_master(job_card_id);
CREATE INDEX idx_production_master_entry_date ON production_master(entry_date);
CREATE INDEX idx_production_master_machine ON production_master(machine_code);
CREATE INDEX idx_production_master_part ON production_master(part_number);
CREATE INDEX idx_production_master_operator ON production_master(employee_number);
CREATE INDEX idx_production_master_shift ON production_master(shift);
CREATE INDEX idx_production_master_validation ON production_master(is_validated);

-- Template Indexes
CREATE INDEX idx_template_field_definitions_template ON template_field_definitions(template_id);
CREATE INDEX idx_template_field_definitions_order ON template_field_definitions(template_id, display_order);
CREATE INDEX idx_field_options_field_def ON field_options(field_definition_id);

-- Work Instructions Indexes
CREATE INDEX idx_work_instructions_job_card ON work_instructions(job_card_id);
CREATE INDEX idx_work_instructions_step_order ON work_instructions(job_card_id, step_number);

-- Assignment Indexes
CREATE INDEX idx_job_card_assignments_job ON job_card_assignments(job_card_id);
CREATE INDEX idx_job_card_assignments_user ON job_card_assignments(assigned_to);
CREATE INDEX idx_job_card_assignments_active ON job_card_assignments(is_active);

-- Audit Indexes
CREATE INDEX idx_job_card_status_history_job ON job_card_status_history(job_card_id);
CREATE INDEX idx_job_card_status_history_date ON job_card_status_history(changed_at);
CREATE INDEX idx_job_card_comments_job ON job_card_comments(job_card_id);

-- ==========================================
-- INITIAL MASTER DATA
-- ==========================================

-- Insert default loss time categories
INSERT INTO loss_time_categories (category_code, category_name, description, color_code) VALUES
('SETUP', 'Setup Time', 'Time spent in machine/tooling setup', '#FF9800'),
('BREAKDOWN', 'Machine Breakdown', 'Unplanned machine downtime', '#F44336'),
('MATERIAL', 'Material Shortage', 'Waiting for material/components', '#9C27B0'),
('QUALITY', 'Quality Issues', 'Time lost due to quality problems', '#E91E63'),
('MAINTENANCE', 'Planned Maintenance', 'Scheduled maintenance activities', '#2196F3'),
('CHANGEOVER', 'Product Changeover', 'Time for changing product/tooling', '#FF5722'),
('TRAINING', 'Operator Training', 'Training and skill development', '#4CAF50'),
('OTHER', 'Other Losses', 'Miscellaneous time losses', '#607D8B');

-- ==========================================
-- VIEWS FOR REPORTING
-- ==========================================

-- Job Card Summary View
CREATE VIEW v_job_card_summary AS
SELECT
    jc.id,
    jc.job_number,
    jc.title,
    jc.status,
    jc.priority,
    jc.estimated_duration_minutes,
    jc.actual_duration_minutes,
    jc.target_completion_date,
    jc.started_at,
    jc.completed_at,
    jct.template_name,
    jct.category as template_category,
    COUNT(wi.id) as total_work_instructions,
    COUNT(CASE WHEN wi.completed_at IS NOT NULL THEN 1 END) as completed_instructions,
    CASE
        WHEN COUNT(wi.id) > 0
        THEN ROUND((COUNT(CASE WHEN wi.completed_at IS NOT NULL THEN 1 END)::DECIMAL / COUNT(wi.id)) * 100, 2)
        ELSE 0
    END as completion_percentage
FROM job_cards jc
LEFT JOIN job_card_templates jct ON jc.template_id = jct.id
LEFT JOIN work_instructions wi ON jc.id = wi.job_card_id
GROUP BY jc.id, jct.template_name, jct.category;

-- Production Efficiency View
CREATE VIEW v_production_efficiency AS
SELECT
    pm.id,
    pm.job_card_id,
    jc.job_number,
    pm.machine_code,
    pm.part_number,
    pm.shift,
    pm.entry_date,
    pm.production_quantity,
    pm.total_time_available_minutes,
    pm.net_time_available_minutes,
    pm.operator_efficiency_percentage,
    pm.equipment_efficiency_percentage,
    (pm.loss_time_1_minutes + pm.loss_time_2_minutes + pm.loss_time_3_minutes +
     pm.loss_time_4_minutes + pm.loss_time_5_minutes + pm.loss_time_6_minutes +
     pm.loss_time_7_minutes + pm.loss_time_8_minutes) as total_loss_time_minutes,
    (pm.rejection_inprocess_quantity + pm.rejection_finalstage_quantity) as total_rejection_quantity
FROM production_master pm
JOIN job_cards jc ON pm.job_card_id = jc.id;

-- ==========================================
-- TRIGGERS FOR AUDIT AND AUTOMATION
-- ==========================================

-- Function to update job card status history
CREATE OR REPLACE FUNCTION log_job_card_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO job_card_status_history (job_card_id, from_status, to_status, changed_by)
        VALUES (NEW.id, OLD.status, NEW.status, NEW.updated_by);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for job card status changes
CREATE TRIGGER trg_job_card_status_change
    AFTER UPDATE ON job_cards
    FOR EACH ROW
    EXECUTE FUNCTION log_job_card_status_change();

-- Function to update timestamps
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers for timestamp updates
CREATE TRIGGER trg_job_cards_update_timestamp
    BEFORE UPDATE ON job_cards
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_production_master_update_timestamp
    BEFORE UPDATE ON production_master
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_machines_update_timestamp
    BEFORE UPDATE ON machines
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();