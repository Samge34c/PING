-- Images are versioned; bytes live separately and are never selected by event/list queries.
CREATE TABLE event_floorplans (
 id VARCHAR(36) NOT NULL PRIMARY KEY, event_id BIGINT NOT NULL,
 filename VARCHAR(160) NOT NULL, width INTEGER NOT NULL, height INTEGER NOT NULL,
 content_type VARCHAR(32) NOT NULL, byte_size INTEGER NOT NULL,
 created_at TIMESTAMP(6) NOT NULL, revision BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE
);
CREATE TABLE event_floorplan_images (
 plan_id VARCHAR(36) NOT NULL PRIMARY KEY, image_data MEDIUMBLOB NOT NULL,
 FOREIGN KEY(plan_id) REFERENCES event_floorplans(id) ON DELETE CASCADE
);
CREATE TABLE event_floorplan_positions (
 plan_id VARCHAR(36) NOT NULL, point_id BIGINT NOT NULL,
 x DOUBLE NOT NULL, y DOUBLE NOT NULL,
 PRIMARY KEY(plan_id,point_id),
 FOREIGN KEY(plan_id) REFERENCES event_floorplans(id) ON DELETE CASCADE,
 FOREIGN KEY(point_id) REFERENCES event_meeting_points(id) ON DELETE CASCADE,
 CHECK(x >= 0 AND x <= 1), CHECK(y >= 0 AND y <= 1)
);
ALTER TABLE events ADD COLUMN floorplan_id VARCHAR(36) NULL;
-- Version rows refer to their event, so this pointer is validated under the event lock.
-- Avoid a circular event/version FK that would obstruct existing event lifecycle operations.
ALTER TABLE ping_matches ADD COLUMN floorplan_id VARCHAR(36) NULL;
ALTER TABLE ping_matches ADD COLUMN meeting_point_id BIGINT NULL;
ALTER TABLE ping_matches ADD COLUMN floorplan_x DOUBLE NULL;
ALTER TABLE ping_matches ADD COLUMN floorplan_y DOUBLE NULL;
ALTER TABLE ping_matches ADD CONSTRAINT fk_match_floorplan FOREIGN KEY(floorplan_id) REFERENCES event_floorplans(id);
