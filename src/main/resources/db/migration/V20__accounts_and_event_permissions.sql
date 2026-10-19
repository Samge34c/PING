CREATE TABLE ping_accounts (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, email VARCHAR(254) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL, email_verified BOOLEAN NOT NULL DEFAULT FALSE,
 general_admin BOOLEAN NOT NULL DEFAULT FALSE, auth_epoch BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE TABLE account_participations (
 account_id BIGINT NOT NULL, event_id BIGINT NOT NULL, participant_id BIGINT NOT NULL UNIQUE,
 PRIMARY KEY(account_id,event_id), FOREIGN KEY(account_id) REFERENCES ping_accounts(id),
 FOREIGN KEY(event_id) REFERENCES events(id), FOREIGN KEY(participant_id) REFERENCES participants(id)
);
CREATE TABLE event_administrators (
 account_id BIGINT NOT NULL, event_id BIGINT NOT NULL, PRIMARY KEY(account_id,event_id),
 FOREIGN KEY(account_id) REFERENCES ping_accounts(id), FOREIGN KEY(event_id) REFERENCES events(id)
);
CREATE TABLE external_identities (
 provider VARCHAR(16) NOT NULL, subject VARCHAR(255) NOT NULL, account_id BIGINT NOT NULL,
 PRIMARY KEY(provider,subject), FOREIGN KEY(account_id) REFERENCES ping_accounts(id)
);
CREATE TABLE account_tokens (
 token_hash VARCHAR(64) NOT NULL PRIMARY KEY, account_id BIGINT NOT NULL, purpose VARCHAR(16) NOT NULL,
 expires_at TIMESTAMP(6) NOT NULL, consumed BOOLEAN NOT NULL DEFAULT FALSE,
 FOREIGN KEY(account_id) REFERENCES ping_accounts(id)
);
CREATE TABLE auth_setup_state (id INTEGER NOT NULL PRIMARY KEY, used BOOLEAN NOT NULL DEFAULT FALSE);
INSERT INTO auth_setup_state(id) VALUES(1);
CREATE TABLE auth_audit (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, actor_id BIGINT NOT NULL, action VARCHAR(40) NOT NULL,
 subject_id BIGINT NULL, event_id BIGINT NULL, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE TABLE auth_throttle (bucket_key VARCHAR(64) NOT NULL PRIMARY KEY, hits INTEGER NOT NULL, expires_at TIMESTAMP(6) NOT NULL);
