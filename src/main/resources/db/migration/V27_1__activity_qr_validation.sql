-- DEC-081. Additive only, numbered 27.1 so that V28 stays reserved for the DEC-079 rule and still applies in order.
-- New check method value 'QR_VALIDATION' fits the existing VARCHAR(16) columns (event_activities, ping_matches).

-- A third credential purpose: the activity QR shown by a participant and scanned by authorised staff.
ALTER TABLE purpose_credentials DROP CONSTRAINT chk_credential_purpose;
ALTER TABLE purpose_credentials ADD CONSTRAINT chk_credential_purpose CHECK(purpose IN ('ENCOUNTER','REDEMPTION','ACTIVITY'));

-- One staff validation per encounter. It never grants points by itself: it only releases the existing completion Saga.
CREATE TABLE activity_qr_validations (
 match_id BIGINT NOT NULL PRIMARY KEY,
 event_id BIGINT NOT NULL,
 validated_by BIGINT NOT NULL,
 validated_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY (match_id) REFERENCES ping_matches(id) ON DELETE CASCADE,
 FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
 FOREIGN KEY (validated_by) REFERENCES ping_accounts(id)
);
CREATE INDEX idx_activity_qr_validations_event ON activity_qr_validations(event_id, validated_at);
