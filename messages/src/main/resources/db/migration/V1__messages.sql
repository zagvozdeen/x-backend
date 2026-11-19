CREATE TABLE messages
(
    id         SERIAL PRIMARY KEY,
    author_id  INTEGER     NOT NULL,
    text       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_author_created ON messages (author_id, created_at DESC, id DESC);
