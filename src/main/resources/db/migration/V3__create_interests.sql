CREATE TABLE interests (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    demo BOOLEAN NOT NULL
);
CREATE TABLE participant_interests (
    participant_id BIGINT NOT NULL,
    interest_id BIGINT NOT NULL,
    PRIMARY KEY (participant_id, interest_id),
    CONSTRAINT fk_pi_participant FOREIGN KEY (participant_id) REFERENCES participants(id),
    CONSTRAINT fk_pi_interest FOREIGN KEY (interest_id) REFERENCES interests(id)
);
CREATE INDEX idx_pi_interest ON participant_interests(interest_id);
