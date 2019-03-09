CREATE TABLE locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v1mc(),
    hangout_id UUID REFERENCES hangouts NOT NULL,
    name TEXT NOT NULL,
    created_by UUID REFERENCES users NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE location_responses (
    location_id UUID REFERENCES locations NOT NULL,
    user_id UUID REFERENCES users NOT NULL,
    response user_response NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT location_reponses_non_none_reponse CHECK (response != 'none'::user_response),
    CONSTRAINT location_responses_user_location_unique UNIQUE (location_id, user_id)
);
