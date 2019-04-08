ALTER TABLE hangouts
    ADD COLUMN moment_time TIME WITHOUT TIME ZONE,
    ADD COLUMN moment_time_zone TEXT,
    ADD COLUMN moment_id UUID REFERENCES moments,
    ADD COLUMN location_id UUID REFERENCES locations,
    ADD CONSTRAINT hangouts_moment_qualified CHECK ((moment_time IS NULL) = (moment_time_zone IS NULL) AND (moment_time IS NULL) = (moment_id IS NULL));

DROP INDEX moments_one_locked;

ALTER TABLE moments
    DROP CONSTRAINT moments_moment_qualified,
    DROP COLUMN locked,
    DROP COLUMN moment_time;

DROP INDEX locations_one_locked;

ALTER TABLE locations
    DROP COLUMN locked;
