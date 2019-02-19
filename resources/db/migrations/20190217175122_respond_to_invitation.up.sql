CREATE TYPE invitations_match_type AS ENUM
    ('exact', 'email', 'mobile-number', 'handle');

CREATE TYPE user_response AS ENUM
    ('none', 'positive', 'negative', 'neutral');

ALTER TABLE invitees
    RENAME COLUMN match_type TO match_type_old;

ALTER TABLE invitees
    ADD COLUMN response user_response NOT NULL DEFAULT 'none'::user_response,
    ADD COLUMN match_type invitations_match_type;

UPDATE invitees
SET match_type = (regexp_replace(match_type_old::TEXT, '_', '-'))::invitations_match_type;

UPDATE invitees
SET response = 'positive'
WHERE attending = true;

ALTER TABLE invitees
    ALTER COLUMN match_type SET NOT NULL,
    DROP COLUMN match_type_old,
    DROP COLUMN attending;

DROP TYPE invitees_match_type;

ALTER TABLE invitees RENAME TO invitations;
