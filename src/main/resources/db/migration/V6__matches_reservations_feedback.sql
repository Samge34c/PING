CREATE TABLE ping_matches (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 event_id BIGINT NOT NULL,
 first_id BIGINT NOT NULL,
 second_id BIGINT NOT NULL,
 status VARCHAR(16) NOT NULL,
 first_accepted BOOLEAN NOT NULL DEFAULT FALSE,
 second_accepted BOOLEAN NOT NULL DEFAULT FALSE,
 first_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
 second_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
 first_wait TIMESTAMP(6) NOT NULL,
 second_wait TIMESTAMP(6) NOT NULL,
 common_interests VARCHAR(700) NOT NULL,
 first_interests VARCHAR(700) NOT NULL,
 second_interests VARCHAR(700) NOT NULL,
 meeting_point VARCHAR(160),
 word VARCHAR(80) UNIQUE,
 activity VARCHAR(500),
 created_at TIMESTAMP(6) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT fk_match_event FOREIGN KEY(event_id) REFERENCES events(id),
 CONSTRAINT fk_match_first FOREIGN KEY(first_id) REFERENCES participants(id),
 CONSTRAINT fk_match_second FOREIGN KEY(second_id) REFERENCES participants(id),
 CONSTRAINT chk_match_distinct CHECK(first_id <> second_id),
 CONSTRAINT chk_match_status CHECK(status IN ('OFFERED','ACTIVE','COMPLETED','CANCELLED'))
);
CREATE INDEX idx_match_event ON ping_matches(event_id, status);
CREATE TABLE match_reservations (
 participant_id BIGINT NOT NULL PRIMARY KEY,
 match_id BIGINT NOT NULL,
 CONSTRAINT fk_reserved_participant FOREIGN KEY(participant_id) REFERENCES participants(id),
 CONSTRAINT fk_reserved_match FOREIGN KEY(match_id) REFERENCES ping_matches(id)
);
CREATE TABLE feedback (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 match_id BIGINT NOT NULL,
 participant_id BIGINT NOT NULL,
 rating VARCHAR(16) NOT NULL,
 CONSTRAINT uq_feedback UNIQUE(match_id, participant_id),
 CONSTRAINT fk_feedback_match FOREIGN KEY(match_id) REFERENCES ping_matches(id),
 CONSTRAINT fk_feedback_participant FOREIGN KEY(participant_id) REFERENCES participants(id),
 CONSTRAINT chk_feedback_rating CHECK(rating IN ('USEFUL','NEUTRAL','NOT_USEFUL'))
);
