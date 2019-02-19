CREATE TYPE invitees_match_type AS ENUM
    ('exact', 'email', 'mobile_number', 'handle');

CREATE TABLE invitees (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v1mc(),
    hangout_id UUID NOT NULL REFERENCES hangouts,
    search VARCHAR(255),
    match_type invitees_match_type NOT NULL,
    user_id UUID REFERENCES users,
    attending BOOLEAN NOT NULL DEFAULT FALSE,
    created_by UUID REFERENCES users NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

ALTER TABLE invitees
    ADD CONSTRAINT invitees_search_or_exact_match CHECK (search IS NOT NULL OR match_type = 'exact'),
    ADD CONSTRAINT invitees_unique_hangout_id_search UNIQUE (hangout_id, search),
    ADD CONSTRAINT invitees_unique_hangout_id_user_id UNIQUE (hangout_id, user_id);

CREATE TABLE known_associates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v1mc(),
    user_id UUID NOT NULL REFERENCES users,
    associate_id UUID NOT NULL REFERENCES users,
    created_by UUID REFERENCES users NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

ALTER TABLE known_associates
    ADD CONSTRAINT known_associates_user_id_not_associate_id CHECK (user_id != associate_id);

CREATE UNIQUE INDEX known_associates_unique_user_id_and_associate_id ON known_associates (least(user_id, associate_id), greatest(user_id, associate_id));

DROP INDEX users_handle;
CREATE UNIQUE INDEX users_handle on users (lower(handle));
