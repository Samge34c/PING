-- Public event identifiers, not credentials. Preserve every existing row and version.
ALTER TABLE events ADD COLUMN entry_code VARCHAR(32) NULL;
UPDATE events SET entry_code = CONCAT('PING', id);
ALTER TABLE events MODIFY COLUMN entry_code VARCHAR(32) NOT NULL;
ALTER TABLE events ADD CONSTRAINT uk_events_entry_code UNIQUE (entry_code);
