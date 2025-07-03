-- src/main/resources/db/migration/V3__insert_default_data.sql

-- Insert default permissions
INSERT INTO permissions (id, name, description, resource_type, action, status) VALUES
-- User Management Permissions
(uuid_generate_v4(), 'USER_CREATE', 'Create new users', 'USER_MANAGEMENT', 'CREATE', 'ACTIVE'),
(uuid_generate_v4(), 'USER_READ', 'View user information', 'USER_MANAGEMENT', 'READ', 'ACTIVE'),
(uuid_generate_v4(), 'USER_UPDATE', 'Update user information', 'USER_MANAGEMENT', 'UPDATE', 'ACTIVE'),
(uuid_generate_v4(), 'USER_DELETE', 'Delete users', 'USER_MANAGEMENT', 'DELETE', 'ACTIVE'),
(uuid_generate_v4(), 'USER_APPROVE', 'Approve user registrations', 'USER_MANAGEMENT', 'APPROVE', 'ACTIVE'),
(uuid_generate_v4(), 'USER_SUSPEND', 'Suspend user accounts', 'USER_MANAGEMENT', 'SUSPEND', 'ACTIVE'),

-- Job Card Permissions
(uuid_generate_v4(), 'JOB_CARD_CREATE', 'Create job cards', 'JOB_CARD', 'CREATE', 'ACTIVE'),
(uuid_generate_v4(), 'JOB_CARD_READ', 'View job cards', 'JOB_CARD', 'READ', 'ACTIVE'),
(uuid_generate_v4(), 'JOB_CARD_UPDATE', 'Update job cards', 'JOB_CARD', 'UPDATE', 'ACTIVE'),
(uuid_generate_v4(), 'JOB_CARD_DELETE', 'Delete job cards', 'JOB_CARD', 'DELETE', 'ACTIVE'),
(uuid_generate_v4(), 'JOB_CARD_ASSIGN', 'Assign job cards to personnel', 'JOB_CARD', 'ASSIGN', 'ACTIVE'),
(uuid_generate_v4(), 'JOB_CARD_APPROVE', 'Approve job card completion', 'JOB_CARD', 'APPROVE', 'ACTIVE'),

-- Personnel Permissions
(uuid_generate_v4(), 'PERSONNEL_CREATE', 'Create personnel records', 'PERSONNEL', 'CREATE', 'ACTIVE'),
(uuid_generate_v4(), 'PERSONNEL_READ', 'View personnel information', 'PERSONNEL', 'READ', 'ACTIVE'),
(uuid_generate_v4(), 'PERSONNEL_UPDATE', 'Update personnel information', 'PERSONNEL', 'UPDATE', 'ACTIVE'),
(uuid_generate_v4(), 'PERSONNEL_DELETE', 'Delete personnel records', 'PERSONNEL', 'DELETE', 'ACTIVE'),

-- Equipment Permissions
(uuid_generate_v4(), 'EQUIPMENT_CREATE', 'Create equipment records', 'EQUIPMENT', 'CREATE', 'ACTIVE'),
(uuid_generate_v4(), 'EQUIPMENT_READ', 'View equipment information', 'EQUIPMENT', 'READ', 'ACTIVE'),
(uuid_generate_v4(), 'EQUIPMENT_UPDATE', 'Update equipment information', 'EQUIPMENT', 'UPDATE', 'ACTIVE'),
(uuid_generate_v4(), 'EQUIPMENT_DELETE', 'Delete equipment records', 'EQUIPMENT', 'DELETE', 'ACTIVE'),

-- Reporting Permissions
(uuid_generate_v4(), 'REPORTING_VIEW', 'View reports', 'REPORTING', 'READ', 'ACTIVE'),
(uuid_generate_v4(), 'REPORTING_GENERATE', 'Generate reports', 'REPORTING', 'CREATE', 'ACTIVE'),
(uuid_generate_v4(), 'REPORTING_EXPORT', 'Export reports', 'REPORTING', 'EXPORT', 'ACTIVE'),

-- System Admin Permissions
(uuid_generate_v4(), 'SYSTEM_CONFIG', 'Configure system settings', 'SYSTEM_ADMIN', 'UPDATE', 'ACTIVE'),
(uuid_generate_v4(), 'SYSTEM_MONITOR', 'Monitor system health', 'SYSTEM_ADMIN', 'READ', 'ACTIVE');

-- Insert default roles
INSERT INTO roles (id, name, description, status) VALUES
(uuid_generate_v4(), 'SUPER_ADMIN', 'Super Administrator with full system access', 'ACTIVE'),
(uuid_generate_v4(), 'ADMIN', 'Administrator with user and content management access', 'ACTIVE'),
(uuid_generate_v4(), 'SUPERVISOR', 'Supervisor with job card management and personnel oversight', 'ACTIVE'),
(uuid_generate_v4(), 'TECHNICIAN', 'Technician with job card execution access', 'ACTIVE'),
(uuid_generate_v4(), 'VIEWER', 'Read-only access to view information', 'ACTIVE');

-- Assign permissions to SUPER_ADMIN role (all permissions)
INSERT INTO role_permissions (role_id, permission_id, status)
SELECT r.id, p.id, 'ACTIVE'
FROM roles r, permissions p
WHERE r.name = 'SUPER_ADMIN' AND p.status = 'ACTIVE';

-- Assign permissions to ADMIN role (exclude system admin permissions)
INSERT INTO role_permissions (role_id, permission_id, status)
SELECT r.id, p.id, 'ACTIVE'
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.status = 'ACTIVE'
  AND p.resource_type != 'SYSTEM_ADMIN';

-- Assign permissions to SUPERVISOR role
INSERT INTO role_permissions (role_id, permission_id, status)
SELECT r.id, p.id, 'ACTIVE'
FROM roles r, permissions p
WHERE r.name = 'SUPERVISOR'
  AND p.status = 'ACTIVE'
  AND p.name IN (
    'USER_READ', 'JOB_CARD_CREATE', 'JOB_CARD_READ', 'JOB_CARD_UPDATE',
    'JOB_CARD_ASSIGN', 'JOB_CARD_APPROVE', 'PERSONNEL_READ', 'PERSONNEL_UPDATE',
    'EQUIPMENT_READ', 'EQUIPMENT_UPDATE', 'REPORTING_VIEW', 'REPORTING_GENERATE'
  );

-- Assign permissions to TECHNICIAN role
INSERT INTO role_permissions (role_id, permission_id, status)
SELECT r.id, p.id, 'ACTIVE'
FROM roles r, permissions p
WHERE r.name = 'TECHNICIAN'
  AND p.status = 'ACTIVE'
  AND p.name IN (
    'JOB_CARD_READ', 'JOB_CARD_UPDATE', 'PERSONNEL_READ', 'EQUIPMENT_READ'
  );

-- Assign permissions to VIEWER role
INSERT INTO role_permissions (role_id, permission_id, status)
SELECT r.id, p.id, 'ACTIVE'
FROM roles r, permissions p
WHERE r.name = 'VIEWER'
  AND p.status = 'ACTIVE'
  AND p.action = 'READ';