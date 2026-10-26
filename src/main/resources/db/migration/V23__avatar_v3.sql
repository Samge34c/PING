-- DEC-058: avatar V3 (animal, pelaje y una pieza por slot con su color).
-- Compatibilidad: los avatares existentes quedan en estilo 'preset' y conservan avatar_animal/hat/accessory/background;
-- se siguen mostrando igual hasta que la persona guarda un avatar V3. No hay conversión automática.
ALTER TABLE participants ADD COLUMN avatar_style VARCHAR(8) NOT NULL DEFAULT 'preset';
ALTER TABLE participants ADD COLUMN avatar_v3_animal VARCHAR(16) NULL;
ALTER TABLE participants ADD COLUMN avatar_v3_palette VARCHAR(16) NULL;
ALTER TABLE participants ADD COLUMN avatar_v3_head VARCHAR(16) NULL;
ALTER TABLE participants ADD COLUMN avatar_v3_head_color VARCHAR(16) NULL;
ALTER TABLE participants ADD COLUMN avatar_v3_audio VARCHAR(16) NULL;
ALTER TABLE participants ADD COLUMN avatar_v3_audio_color VARCHAR(16) NULL;
ALTER TABLE participants ADD COLUMN avatar_v3_face VARCHAR(16) NULL;
ALTER TABLE participants ADD COLUMN avatar_v3_face_color VARCHAR(16) NULL;
ALTER TABLE participants ADD COLUMN avatar_v3_neck VARCHAR(16) NULL;
ALTER TABLE participants ADD COLUMN avatar_v3_neck_color VARCHAR(16) NULL;
