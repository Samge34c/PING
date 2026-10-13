ALTER TABLE ping_matches ADD COLUMN completion_pending BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE match_completion_sagas (
 match_id BIGINT NOT NULL PRIMARY KEY,
 status VARCHAR(32) NOT NULL,
 attempt INTEGER NOT NULL,
 trigger_participant_id BIGINT NOT NULL,
 first_granted BOOLEAN NOT NULL DEFAULT FALSE,
 second_granted BOOLEAN NOT NULL DEFAULT FALSE,
 failure_code VARCHAR(80),
 CONSTRAINT fk_completion_match FOREIGN KEY(match_id) REFERENCES ping_matches(id) ON DELETE CASCADE,
 CONSTRAINT chk_completion_attempt CHECK(attempt > 0)
);
CREATE TABLE match_completion_steps (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 match_id BIGINT NOT NULL,
 attempt INTEGER NOT NULL,
 action VARCHAR(32) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT fk_completion_step FOREIGN KEY(match_id) REFERENCES match_completion_sagas(match_id) ON DELETE CASCADE,
 CONSTRAINT uq_completion_action UNIQUE(match_id, attempt, action)
);
