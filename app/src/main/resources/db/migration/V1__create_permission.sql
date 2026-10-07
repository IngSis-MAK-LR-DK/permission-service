CREATE TABLE permission (
    id         BIGSERIAL PRIMARY KEY,
    snippet_id VARCHAR(255) NOT NULL,
    user_id    VARCHAR(255) NOT NULL,
    role       VARCHAR(16)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_permission_snippet_user UNIQUE (snippet_id, user_id)
);

CREATE INDEX idx_permission_user ON permission (user_id);
