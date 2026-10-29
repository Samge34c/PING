-- Optional additions. Existing events need no new settings or agenda rows.
CREATE TABLE event_experience_settings (
 event_id BIGINT NOT NULL PRIMARY KEY,
 zone_id VARCHAR(64) NOT NULL DEFAULT 'America/Bogota',
 staff_location VARCHAR(240) NOT NULL DEFAULT '',
 official_contact VARCHAR(240) NOT NULL DEFAULT '',
 challenge_enabled BOOLEAN NOT NULL DEFAULT FALSE,
 version BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY (event_id) REFERENCES events(id)
);
CREATE TABLE event_agenda_items (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 event_id BIGINT NOT NULL,
 request_key VARCHAR(36) NOT NULL,
 title VARCHAR(120) NOT NULL,
 description VARCHAR(500) NOT NULL DEFAULT '',
 starts_at BIGINT NOT NULL,
 ends_at BIGINT NULL,
 zone_id VARCHAR(64) NOT NULL,
 place VARCHAR(160) NOT NULL DEFAULT '',
 point_id BIGINT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 version BIGINT NOT NULL DEFAULT 0,
 UNIQUE (event_id, request_key),
 FOREIGN KEY (event_id) REFERENCES events(id),
 FOREIGN KEY (point_id) REFERENCES event_meeting_points(id)
);
CREATE INDEX idx_agenda_event_time ON event_agenda_items(event_id, active, starts_at, id);
