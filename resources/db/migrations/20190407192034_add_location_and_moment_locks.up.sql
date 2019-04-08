ALTER TABLE hangouts
    DROP CONSTRAINT hangouts_moment_qualified,
    DROP COLUMN moment_time,
    DROP COLUMN moment_time_zone,
    DROP COLUMN moment_id,
    DROP COLUMN location_id;

ALTER TABLE moments
    ADD COLUMN locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN moment_time TIME WITHOUT TIME ZONE,
    ADD CONSTRAINT moments_moment_qualified CHECK ((moment_time IS NULL) OR (locked = FALSE));

CREATE UNIQUE INDEX moments_one_locked ON moments (hangout_id, (CASE WHEN locked THEN TRUE END));

ALTER TABLE locations
    ADD COLUMN locked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX locations_one_locked ON locations (hangout_id, (CASE WHEN locked THEN TRUE END));
