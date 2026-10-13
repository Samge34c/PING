-- Marcador del escenario manual. No crea eventos ni asistentes automáticamente.
CREATE TABLE demo_scenarios (
 id BIGINT NOT NULL PRIMARY KEY,
 event_id BIGINT NULL,
 CONSTRAINT fk_demo_event FOREIGN KEY(event_id) REFERENCES events(id)
);
INSERT INTO demo_scenarios(id, event_id) VALUES (1, NULL);
