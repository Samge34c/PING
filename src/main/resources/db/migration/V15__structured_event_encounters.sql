ALTER TABLE events ADD COLUMN structured_encounter BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE event_meeting_points (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, event_id BIGINT NOT NULL,
 name VARCHAR(160) NOT NULL, directions VARCHAR(500) NOT NULL, active BOOLEAN NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE
);
CREATE TABLE event_activities (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, event_id BIGINT NOT NULL,
 title VARCHAR(120) NOT NULL, instructions VARCHAR(500) NOT NULL, active BOOLEAN NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE
);
CREATE TABLE event_activity_points (
 activity_id BIGINT NOT NULL, point_id BIGINT NOT NULL, PRIMARY KEY(activity_id,point_id),
 FOREIGN KEY(activity_id) REFERENCES event_activities(id) ON DELETE CASCADE,
 FOREIGN KEY(point_id) REFERENCES event_meeting_points(id) ON DELETE CASCADE
);
CREATE TABLE event_activity_interests (
 activity_id BIGINT NOT NULL, interest_id BIGINT NOT NULL, PRIMARY KEY(activity_id,interest_id),
 FOREIGN KEY(activity_id) REFERENCES event_activities(id) ON DELETE CASCADE,
 FOREIGN KEY(interest_id) REFERENCES interests(id)
);
ALTER TABLE ping_matches ADD COLUMN meeting_directions VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE ping_matches ADD COLUMN activity_title VARCHAR(120) NOT NULL DEFAULT '';
