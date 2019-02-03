ALTER TABLE events RENAME TO hangouts;
ALTER INDEX events_pkey RENAME TO hangouts_pkey;
ALTER TABLE hangouts RENAME CONSTRAINT events_created_by_fkey TO hangouts_created_by_fkey;
