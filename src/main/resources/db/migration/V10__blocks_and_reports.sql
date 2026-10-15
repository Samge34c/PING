CREATE TABLE participant_blocks (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 blocker_id BIGINT NOT NULL,
 blocked_id BIGINT NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT uq_block_pair UNIQUE(blocker_id, blocked_id),
 CONSTRAINT fk_block_author FOREIGN KEY(blocker_id) REFERENCES participants(id) ON DELETE CASCADE,
 CONSTRAINT fk_block_target FOREIGN KEY(blocked_id) REFERENCES participants(id) ON DELETE CASCADE,
 CONSTRAINT chk_block_distinct CHECK(blocker_id <> blocked_id)
);
CREATE INDEX idx_block_target ON participant_blocks(blocked_id);
CREATE TABLE interaction_reports (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 match_id BIGINT NOT NULL,
 reporter_id BIGINT NOT NULL,
 reason VARCHAR(24) NOT NULL,
 comment VARCHAR(300) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT uq_report_author UNIQUE(match_id, reporter_id),
 CONSTRAINT fk_report_match FOREIGN KEY(match_id) REFERENCES ping_matches(id) ON DELETE CASCADE,
 CONSTRAINT fk_report_author FOREIGN KEY(reporter_id) REFERENCES participants(id) ON DELETE CASCADE,
 CONSTRAINT chk_report_reason CHECK(reason IN ('UNCOMFORTABLE','NO_SHOW','INAPPROPRIATE','OTHER'))
);
