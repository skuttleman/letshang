DROP INDEX locations_hangout_id_name;

ALTER TABLE locations
    DROP CONSTRAINT locations_name_trimmed;
