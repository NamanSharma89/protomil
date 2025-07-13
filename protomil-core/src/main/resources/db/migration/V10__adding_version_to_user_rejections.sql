-- Add missing version column to user_rejections table for optimistic locking
ALTER TABLE user_rejections
ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Update existing rows to have version = 0
UPDATE user_rejections SET version = 0 WHERE version IS NULL;

-- Make version column NOT NULL
ALTER TABLE user_rejections
ALTER COLUMN version SET NOT NULL;