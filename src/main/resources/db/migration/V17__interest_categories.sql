CREATE TABLE interest_categories (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(80) NOT NULL UNIQUE,
 presentation_order INTEGER NOT NULL DEFAULT 0,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 version BIGINT NOT NULL DEFAULT 0
);
ALTER TABLE interests ADD COLUMN category_id BIGINT NULL;
ALTER TABLE interests ADD CONSTRAINT fk_interest_category FOREIGN KEY(category_id) REFERENCES interest_categories(id) ON DELETE SET NULL;
CREATE INDEX idx_interest_category ON interests(category_id);
CREATE TABLE interest_catalog_state (id BIGINT NOT NULL PRIMARY KEY);
INSERT INTO interest_catalog_state(id) VALUES(1);
INSERT INTO interest_categories(id,name,presentation_order,active) VALUES
 (1,'Cultura y entretenimiento',10,TRUE),
 (2,'Creatividad y tecnología',20,TRUE),
 (3,'Vida activa y viajes',30,TRUE);
UPDATE interests SET category_id=1 WHERE name IN ('Videojuegos','Música','Cine','Anime');
UPDATE interests SET category_id=2 WHERE name IN ('Tecnología','Fotografía');
UPDATE interests SET category_id=3 WHERE name IN ('Deportes','Viajes');
