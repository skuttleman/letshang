DROP TABLE events;

ALTER TABLE users
    DROP CONSTRAINT users_valid_email,
    DROP CONSTRAINT users_valid_phone;

DROP INDEX users_handle;
DROP INDEX users_email;
DROP INDEX users_mobile_number;

DROP TABLE users;

DROP EXTENSION "uuid-ossp";
