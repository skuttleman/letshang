CREATE TABLE sessions (
    id VARCHAR(80) PRIMARY KEY,
    user_id UUID REFERENCES users NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX sessions_user_id ON sessions (user_id);
