CREATE EXTENSION "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v1mc(),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    handle VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    mobile_number VARCHAR(10) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

ALTER TABLE users
    ADD CONSTRAINT users_valid_email CHECK (email ~ '^[a-z\-\+_0-9\.]+@[a-z\-\+_0-9]+\.[a-z\-\+_0-9\.]+$'),
    ADD CONSTRAINT users_valid_phone CHECK (mobile_number ~ '^\d{10}$');

CREATE UNIQUE INDEX users_handle on users (handle);
CREATE UNIQUE INDEX users_email on users (email);
CREATE UNIQUE INDEX users_mobile_number on users (mobile_number);

CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v1mc(),
    name VARCHAR(255) NOT NULL,
    created_by UUID REFERENCES users NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);
