-- DEC-079 (propuesta). Additive and optional: total of people who entered the venue through the door,
-- counted outside PING by the organisation. NULL = not recorded. Existing events are not back-filled.
ALTER TABLE events ADD COLUMN door_attendance INTEGER NULL;
