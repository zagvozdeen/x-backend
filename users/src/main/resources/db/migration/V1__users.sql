CREATE TABLE users
(
    id                    SERIAL PRIMARY KEY,
    first_name            VARCHAR(255) NOT NULL,
    last_name             VARCHAR(255) NOT NULL,
    email                 VARCHAR(255) NOT NULL UNIQUE CHECK (email = lower(email)),
    phone_number          VARCHAR(20)  NULL UNIQUE,
    password              VARCHAR(255) NOT NULL,
    email_confirmed_at    TIMESTAMPTZ  NULL,
    email_confirmation_id UUID         NULL UNIQUE,
    auth_version          INTEGER      NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_email_confirmation_state CHECK ((email_confirmed_at IS NULL) <> (email_confirmation_id IS NULL))
);

CREATE TABLE roles
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE user_role
(
    user_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles (id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO roles (name)
VALUES ('user'),
       ('producer'),
       ('subscriber'),
       ('admin');
