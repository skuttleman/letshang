ALTER TABLE hangouts
    ADD COLUMN moment_time TIME WITHOUT TIME ZONE,
    ADD COLUMN moment_time_zone TEXT,
    ADD COLUMN moment_id UUID REFERENCES moments,
    ADD COLUMN location_id UUID REFERENCES locations,
    ADD COLUMN others_invite BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN when_suggestions BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN where_suggestions BOOLEAN NOT NULL DEFAULT true,
    ADD CONSTRAINT hangouts_moment_qualified CHECK ((moment_time IS NULL) = (moment_time_zone IS NULL) AND (moment_time IS NULL) = (moment_id IS NULL));
