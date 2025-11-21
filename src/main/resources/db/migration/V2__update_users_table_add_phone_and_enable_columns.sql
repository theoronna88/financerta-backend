ALTER TABLE users
    ADD COLUMN phone VARCHAR(20),
    ADD COLUMN enabled BOOLEAN DEFAULT TRUE;

CREATE INDEX idx_users_phone ON users(phone);