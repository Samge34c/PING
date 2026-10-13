CREATE TABLE benefits (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(120) NOT NULL,
 points_cost BIGINT NOT NULL,
 active BOOLEAN NOT NULL,
 CONSTRAINT chk_benefit_cost CHECK(points_cost > 0)
);
CREATE TABLE reward_claims (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 participant_id BIGINT NOT NULL,
 benefit_id BIGINT NOT NULL,
 request_key VARCHAR(36) NOT NULL,
 cost BIGINT NOT NULL,
 status VARCHAR(16) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT uq_claim_request UNIQUE(participant_id, request_key),
 CONSTRAINT fk_claim_participant FOREIGN KEY(participant_id) REFERENCES participants(id) ON DELETE CASCADE,
 CONSTRAINT fk_claim_benefit FOREIGN KEY(benefit_id) REFERENCES benefits(id),
 CONSTRAINT chk_claim_cost CHECK(cost > 0),
 CONSTRAINT chk_claim_status CHECK(status IN ('REQUESTED','DELIVERED','INCIDENT'))
);
INSERT INTO benefits(name, points_cost, active) VALUES ('Snack del evento · DEMO', 20, TRUE);
