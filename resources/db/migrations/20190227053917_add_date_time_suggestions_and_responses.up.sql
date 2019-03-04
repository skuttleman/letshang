CREATE TYPE moments_window AS ENUM
    ('any-time', 'morning', 'mid-day', 'afternoon', 'after-work', 'evening', 'night', 'twilight');

CREATE TABLE moments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v1mc(),
    hangout_id UUID REFERENCES hangouts NOT NULL,
    date DATE NOT NULL,
    moment_window moments_window NOT NULL,
    created_by UUID REFERENCES users NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT moments_unique_hangout_id_date_window UNIQUE (hangout_id, date, moment_window)
);

CREATE TABLE moment_responses (
    moment_id UUID REFERENCES moments NOT NULL,
    user_id UUID REFERENCES users NOT NULL,
    response user_response NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT moment_reponses_non_none_reponse CHECK (response != 'none'::user_response),
    CONSTRAINT moment_responses_user_moment_unique UNIQUE (moment_id, user_id)
);
