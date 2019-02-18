ALTER TABLE invitations RENAME TO invitees;

CREATE TYPE invitees_match_type AS ENUM
    ('exact', 'email', 'mobile_number', 'handle');

ALTER TABLE invitees
    RENAME COLUMN match_type TO match_type_old;

ALTER TABLE invitees
    ADD COLUMN attending BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN match_type invitees_match_type;

UPDATE invitees
SET match_type = (regexp_replace(match_type_old::TEXT, '-', '_'))::invitees_match_type;

UPDATE invitees
SET attending = true
WHERE response = 'positive';

ALTER TABLE invitees
    ALTER COLUMN match_type SET NOT NULL,
    DROP COLUMN match_type_old,
    DROP COLUMN response;

DROP TYPE user_response;

DROP TYPE invitations_match_type;
