-- Policy1 preserves existing ACTIVE/COMPLETED/Saga conditions. New activation explicitly freezes2.
ALTER TABLE ping_matches ADD COLUMN verification_policy INTEGER NOT NULL DEFAULT 1;
ALTER TABLE ping_matches ADD COLUMN first_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ping_matches ADD COLUMN second_verified BOOLEAN NOT NULL DEFAULT FALSE;
-- The expanded editorial catalog must not truncate explicit interest snapshots.
ALTER TABLE ping_matches MODIFY COLUMN common_interests TEXT NOT NULL;
ALTER TABLE ping_matches MODIFY COLUMN first_interests TEXT NOT NULL;
ALTER TABLE ping_matches MODIFY COLUMN second_interests TEXT NOT NULL;
ALTER TABLE reward_claims ADD COLUMN receipt_policy INTEGER NOT NULL DEFAULT 1;
CREATE TABLE purpose_credentials (
 id VARCHAR(36) NOT NULL PRIMARY KEY, issue_key VARCHAR(36) NOT NULL UNIQUE,
 purpose VARCHAR(16) NOT NULL, context_id BIGINT NOT NULL, event_id BIGINT NOT NULL,
 issuer_id BIGINT NOT NULL, recipient_id BIGINT NULL,
 token_hash VARCHAR(64) NOT NULL UNIQUE, manual_hash VARCHAR(64) NOT NULL UNIQUE,
 expires_at TIMESTAMP(6) NOT NULL, consumed BOOLEAN NOT NULL DEFAULT FALSE,
 revoked BOOLEAN NOT NULL DEFAULT FALSE,
 FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE,
 FOREIGN KEY(issuer_id) REFERENCES participants(id) ON DELETE CASCADE,
 FOREIGN KEY(recipient_id) REFERENCES participants(id) ON DELETE CASCADE,
 CONSTRAINT chk_credential_purpose CHECK(purpose IN ('ENCOUNTER','REDEMPTION'))
);
CREATE INDEX idx_credentials_context ON purpose_credentials(purpose,context_id,issuer_id);
