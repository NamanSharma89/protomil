-- src/main/resources/db/migration/V1_5_2__Create_User_Rejections_Table.sql
-- Create table to track user rejection history

CREATE TABLE IF NOT EXISTS user_rejections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rejected_by UUID NOT NULL REFERENCES users(id),
    rejection_reason TEXT NOT NULL,
    rejected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    original_status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_user_rejections_user_id ON user_rejections(user_id);
CREATE INDEX IF NOT EXISTS idx_user_rejections_rejected_by ON user_rejections(rejected_by);
CREATE INDEX IF NOT EXISTS idx_user_rejections_rejected_at ON user_rejections(rejected_at);

-- Add comments for documentation
COMMENT ON TABLE user_rejections IS 'Tracks user rejection history by administrators';
COMMENT ON COLUMN user_rejections.rejection_reason IS 'Administrator provided reason for rejection';
COMMENT ON COLUMN user_rejections.original_status IS 'User status before rejection';

-- src/main/resources/db/migration/V1_5_3__Create_Default_Pending_User_Role.sql
-- Create PENDING_USER role for new users

INSERT INTO roles (id, name, description, status, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'PENDING_USER',
    'Default role for users awaiting role assignment - access to dashboard and profile only',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (name) DO NOTHING;