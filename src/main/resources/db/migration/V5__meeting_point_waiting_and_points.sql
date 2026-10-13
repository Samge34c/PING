ALTER TABLE events ADD COLUMN meeting_point VARCHAR(160) NOT NULL DEFAULT 'Punto Ping del evento';
ALTER TABLE participants ADD COLUMN waiting_since TIMESTAMP(6) NULL;
ALTER TABLE participants ADD COLUMN points BIGINT NOT NULL DEFAULT 0;
ALTER TABLE participants ADD CONSTRAINT chk_points_nonnegative CHECK (points >= 0);
UPDATE participants SET waiting_since = created_at WHERE available = TRUE;
CREATE INDEX idx_participant_waiting ON participants(event_id, available, waiting_since, id);
