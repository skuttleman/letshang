ALTER TABLE locations
    ADD CONSTRAINT locations_name_trimmed CHECK (name = trim(name));

CREATE UNIQUE INDEX locations_hangout_id_name ON locations (hangout_id, LOWER(name));
