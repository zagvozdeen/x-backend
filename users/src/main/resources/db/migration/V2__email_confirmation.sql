ALTER TABLE users
    ADD COLUMN email_confirmed_at TIMESTAMPTZ,
    ADD COLUMN email_confirmation_id UUID UNIQUE,
    ADD COLUMN auth_version INTEGER NOT NULL DEFAULT 0;

-- Эти аккаунты могли входить до появления подтверждения email.
UPDATE users SET email_confirmed_at = CURRENT_TIMESTAMP;

ALTER TABLE users
    ADD CONSTRAINT users_email_confirmation_state
        CHECK ((email_confirmed_at IS NULL) <> (email_confirmation_id IS NULL));
