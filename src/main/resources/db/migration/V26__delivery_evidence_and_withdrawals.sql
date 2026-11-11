-- DEC-068. Additive only: existing rows keep their behaviour.
-- New check method value 'AT_DELIVERY' fits the existing VARCHAR(16) columns; no change needed there.

-- Points granted by an AT_DELIVERY encounter: they are part of participants.points (single balance)
-- and stay "to check at delivery" while the encounter has no evidence validation.
-- unspent = part of this credit not yet used by a receipt.
CREATE TABLE evidence_credits (
 match_id BIGINT NOT NULL,
 participant_id BIGINT NOT NULL,
 amount INTEGER NOT NULL,
 unspent INTEGER NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 PRIMARY KEY (match_id, participant_id),
 FOREIGN KEY (match_id) REFERENCES ping_matches(id),
 FOREIGN KEY (participant_id) REFERENCES participants(id)
);
CREATE INDEX idx_evidence_credits_participant ON evidence_credits(participant_id);

-- One in-person validation per encounter: it covers both participants. Never grants points.
CREATE TABLE evidence_validations (
 match_id BIGINT NOT NULL PRIMARY KEY,
 event_id BIGINT NOT NULL,
 validated_by BIGINT NOT NULL,
 validated_at TIMESTAMP(6) NOT NULL,
 claim_id BIGINT NOT NULL,
 FOREIGN KEY (match_id) REFERENCES ping_matches(id),
 FOREIGN KEY (event_id) REFERENCES events(id),
 FOREIGN KEY (validated_by) REFERENCES ping_accounts(id),
 FOREIGN KEY (claim_id) REFERENCES reward_claims(id)
);

-- Which still-unchecked points each receipt used (written once, when the receipt is paid).
CREATE TABLE claim_evidence (
 claim_id BIGINT NOT NULL,
 match_id BIGINT NOT NULL,
 amount INTEGER NOT NULL,
 PRIMARY KEY (claim_id, match_id),
 FOREIGN KEY (claim_id) REFERENCES reward_claims(id),
 FOREIGN KEY (match_id) REFERENCES ping_matches(id)
);

-- "Evidence missing" notes: the receipt stays REQUESTED, nothing is delivered or charged.
CREATE TABLE claim_evidence_notes (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 claim_id BIGINT NOT NULL,
 staff_id BIGINT NOT NULL,
 note VARCHAR(300) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY (claim_id) REFERENCES reward_claims(id),
 FOREIGN KEY (staff_id) REFERENCES ping_accounts(id)
);
CREATE INDEX idx_claim_evidence_notes_claim ON claim_evidence_notes(claim_id, id);

-- Per-event role. Every existing permission keeps full organisation rights.
ALTER TABLE event_administrators ADD COLUMN admin_role VARCHAR(16) NOT NULL DEFAULT 'ORGANIZER';

-- Withdrawal from one event (participation row). Account, history, points and deliveries are kept.
ALTER TABLE participants ADD COLUMN withdrawn BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE participant_withdrawals (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 participant_id BIGINT NOT NULL,
 event_id BIGINT NOT NULL,
 action VARCHAR(16) NOT NULL,
 actor_id BIGINT NOT NULL,
 reason VARCHAR(300) NOT NULL DEFAULT '',
 created_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY (participant_id) REFERENCES participants(id),
 FOREIGN KEY (event_id) REFERENCES events(id),
 FOREIGN KEY (actor_id) REFERENCES ping_accounts(id)
);
CREATE INDEX idx_participant_withdrawals_participant ON participant_withdrawals(participant_id, id);

-- The partner of a withdrawn person sees that the organisation cancelled the encounter (never the reason).
ALTER TABLE ping_matches ADD COLUMN cancelled_by_organization BOOLEAN NOT NULL DEFAULT FALSE;

-- A receipt still to deliver is cancelled (and refunded once) when its participant is withdrawn.
-- DROP CONSTRAINT works in MySQL 8.0.19+ and H2; existing statuses stay valid.
ALTER TABLE reward_claims DROP CONSTRAINT chk_claim_status;
ALTER TABLE reward_claims ADD CONSTRAINT chk_claim_status CHECK(status IN ('REQUESTED','DELIVERED','INCIDENT','CANCELLED'));
