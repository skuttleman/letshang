DROP TABLE known_associates;
DROP TABLE invitees;
DROP TYPE invitees_match_type;

DROP INDEX users_handle;
CREATE UNIQUE INDEX users_handle on users (handle);
