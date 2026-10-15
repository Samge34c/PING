-- Optional private presentation text. No backfill or reset of existing participants.
ALTER TABLE participants ADD COLUMN recognition_hint VARCHAR(120) NULL;
