-- DEC-066. Additive only: existing rows keep the bilateral check and no reward image.
-- How an activity is checked. BILATERAL keeps the previous behaviour (both confirm).
ALTER TABLE event_activities ADD COLUMN verification_method VARCHAR(16) NOT NULL DEFAULT 'BILATERAL';
ALTER TABLE event_activities ADD COLUMN check_instructions VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE event_activities ADD COLUMN response_prompt VARCHAR(200) NOT NULL DEFAULT '';

-- Snapshot frozen when an encounter is activated; earlier encounters stay BILATERAL.
ALTER TABLE ping_matches ADD COLUMN activity_check VARCHAR(16) NOT NULL DEFAULT 'BILATERAL';
ALTER TABLE ping_matches ADD COLUMN check_instructions VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE ping_matches ADD COLUMN response_prompt VARCHAR(200) NOT NULL DEFAULT '';

-- One shared submission per encounter. Saving or approving never grants points by itself:
-- points still come only from the completion Saga after both confirmations.
CREATE TABLE activity_reviews (
 match_id BIGINT NOT NULL PRIMARY KEY,
 event_id BIGINT NOT NULL,
 status VARCHAR(16) NOT NULL,
 response VARCHAR(1000) NOT NULL DEFAULT '',
 submitted_by BIGINT NOT NULL,
 submitted_at TIMESTAMP(6) NOT NULL,
 reviewer_account_id BIGINT NULL,
 reviewer_comment VARCHAR(500) NOT NULL DEFAULT '',
 reviewed_at TIMESTAMP(6) NULL,
 revision BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY (match_id) REFERENCES ping_matches(id),
 FOREIGN KEY (event_id) REFERENCES events(id),
 FOREIGN KEY (submitted_by) REFERENCES participants(id)
);
CREATE INDEX idx_activity_reviews_event ON activity_reviews(event_id, status, submitted_at);

-- Optional reward image. Lists read only the revision; bytes live in their own table.
ALTER TABLE benefits ADD COLUMN image_revision BIGINT NULL;
CREATE TABLE benefit_images (
 benefit_id BIGINT NOT NULL PRIMARY KEY,
 content_type VARCHAR(32) NOT NULL,
 width INTEGER NOT NULL,
 height INTEGER NOT NULL,
 byte_size INTEGER NOT NULL,
 image_data MEDIUMBLOB NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY (benefit_id) REFERENCES benefits(id) ON DELETE CASCADE
);
