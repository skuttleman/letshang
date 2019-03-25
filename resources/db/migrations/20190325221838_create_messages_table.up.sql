CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v1mc(),
    hangout_id UUID REFERENCES hangouts NOT NULL,
    body TEXT NOT NULL,
    created_by UUID REFERENCES users NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);
