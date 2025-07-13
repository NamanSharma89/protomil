-- Missing from UserStatus enum
ALTER TYPE user_status ADD VALUE 'REJECTED';

-- Missing rejection tracking table
CREATE TABLE user_rejections (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    rejected_by UUID NOT NULL REFERENCES users(id),
    rejection_reason TEXT NOT NULL,
    rejected_at TIMESTAMP NOT NULL,
    original_status VARCHAR(50) NOT NULL
);