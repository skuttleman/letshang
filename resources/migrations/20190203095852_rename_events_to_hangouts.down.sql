ALTER TABLE hangouts RENAME TO events;
ALTER INDEX hangouts_pkey RENAME TO events_pkey;
ALTER TABLE events RENAME CONSTRAINT hangouts_created_by_fkey TO events_created_by_fkey;
