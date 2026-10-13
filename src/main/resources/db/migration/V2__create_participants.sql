CREATE TABLE participants (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    alias VARCHAR(80) NOT NULL,
    event_id BIGINT NOT NULL,
    available BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_participant_event FOREIGN KEY (event_id) REFERENCES events(id)
);
CREATE INDEX idx_participant_event ON participants(event_id);
