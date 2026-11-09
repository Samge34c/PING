package com.ping.reward;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import javax.imageio.*;
import javax.imageio.stream.*;
import com.ping.event.EventService;
import com.ping.web.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * DEC-066. Optional reward image stored in MySQL (Render's disk is ephemeral), apart from the benefit row:
 * lists only read {@code benefits.image_revision}. Uploads are decoded and re-encoded server-side, which
 * validates the real type, applies EXIF orientation and drops every metadata block.
 */
@Service @Profile("!bootstrap") @Transactional(readOnly = true)
public class BenefitImageService {
    public static final int MAX_INPUT = 8 * 1024 * 1024, MAX_SIDE = 1200, MAX_STORED = 700 * 1024;
    public record Stored(String contentType, byte[] bytes, long revision) { }
    private final JdbcTemplate sql;
    private final EventService events;
    private final BenefitRepository benefits;
    public BenefitImageService(JdbcTemplate sql, EventService events, BenefitRepository benefits) { this.sql = sql; this.events = events; this.benefits = benefits; }

    private Benefit owned(long event, long id) {
        var b = benefits.findById(id).orElseThrow(() -> new NotFoundException("No encontramos esa recompensa."));
        if (b.getEventId() == null || b.getEventId() != event) throw new NotFoundException("La recompensa no pertenece a este evento.");
        return b;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void save(long event, long id, MultipartFile file) {
        var image = encode(file); // decode outside the event lock
        events.lock(event); var b = owned(event, id);
        long revision = Math.max(Instant.now().toEpochMilli(), b.getImageRevision() == null ? 0 : b.getImageRevision() + 1);
        if (sql.update("update benefit_images set content_type=?,width=?,height=?,byte_size=?,image_data=?,updated_at=? where benefit_id=?",
                "image/jpeg", image.getWidth(), image.getHeight(), image.bytes.length, image.bytes, Timestamp.from(Instant.now()), id) == 0)
            sql.update("insert into benefit_images(benefit_id,content_type,width,height,byte_size,image_data,updated_at) values(?,?,?,?,?,?,?)",
                    id, "image/jpeg", image.getWidth(), image.getHeight(), image.bytes.length, image.bytes, Timestamp.from(Instant.now()));
        sql.update("update benefits set image_revision=?,version=version+1 where id=?", revision, id);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void remove(long event, long id) {
        events.lock(event); owned(event, id);
        sql.update("delete from benefit_images where benefit_id=?", id);
        sql.update("update benefits set image_revision=null,version=version+1 where id=?", id);
    }

    /** Bytes for one benefit; the caller has already checked who may see it. */
    public Stored read(long id) {
        return sql.query("select i.content_type,i.image_data,b.image_revision from benefit_images i join benefits b on b.id=i.benefit_id where i.benefit_id=?",
                (r, n) -> new Stored(r.getString(1), r.getBytes(2), r.getLong(3)), id).stream().findFirst()
            .orElseThrow(() -> new NotFoundException("Esta recompensa no tiene imagen."));
    }
    public Benefit benefit(long id) { return benefits.findById(id).orElseThrow(() -> new NotFoundException("No encontramos esa recompensa.")); }
    public void requireOwned(long event, long id) { owned(event, id); }

    private record Encoded(BufferedImage image, byte[] bytes) { int getWidth() { return image.getWidth(); } int getHeight() { return image.getHeight(); } }

    Encoded encode(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("Elige una imagen PNG, JPEG o WebP.");
        if (file.getSize() > MAX_INPUT) throw invalid("La imagen supera 8 MiB. Elige una versión más ligera.");
        try (var input = file.getInputStream()) {
            byte[] data = input.readNBytes(MAX_INPUT + 1);
            if (data.length > MAX_INPUT) throw invalid("La imagen supera 8 MiB.");
            BufferedImage original; String format;
            try (var stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
                var readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw invalid("No pudimos leer esa imagen. Usa PNG, JPEG o WebP; no SVG ni otros formatos.");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(stream);
                    format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!java.util.Set.of("png", "jpeg", "jpg", "webp").contains(format)) throw invalid("Solo se admiten PNG, JPEG o WebP.");
                    int w = reader.getWidth(0), h = reader.getHeight(0);
                    // Bound decoding work before allocating pixels.
                    if (w < 160 || h < 160 || w > 8000 || h > 8000 || (long) w * h > 24_000_000) throw invalid("Usa una imagen de al menos 160 px por lado y hasta 24 megapíxeles.");
                    if (reader.getNumImages(true) != 1) throw invalid("Usa una imagen fija, sin animación.");
                    original = reader.read(0);
                } finally { reader.dispose(); }
            }
            if (original == null) throw invalid("La imagen está dañada. Vuelve a exportarla.");
            int orientation = format.startsWith("jp") ? exifOrientation(data) : 1;
            BufferedImage upright = orient(original, orientation);
            double factor = Math.min(1, (double) MAX_SIDE / Math.max(upright.getWidth(), upright.getHeight()));
            int width = Math.max(1, (int) Math.round(upright.getWidth() * factor)), height = Math.max(1, (int) Math.round(upright.getHeight() * factor));
            BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            try {
                g.setColor(Color.WHITE); g.fillRect(0, 0, width, height);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(upright, 0, 0, width, height, null);
            } finally { g.dispose(); original.flush(); upright.flush(); }
            for (float quality : new float[] {.85f, .75f, .65f}) {
                byte[] bytes = jpeg(out, quality);
                if (bytes.length <= MAX_STORED) return new Encoded(out, bytes);
            }
            throw invalid("La imagen sigue siendo demasiado pesada tras optimizarla. Usa una foto más sencilla.");
        } catch (IOException | IllegalArgumentException e) {
            throw invalid("La imagen no se pudo decodificar. Vuelve a exportarla como PNG, JPEG o WebP.");
        }
    }

    private static byte[] jpeg(BufferedImage image, float quality) throws IOException {
        var output = new ByteArrayOutputStream(); var writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (var target = new MemoryCacheImageOutputStream(output)) {
            writer.setOutput(target); var options = writer.getDefaultWriteParam();
            options.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); options.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), options); // no metadata is written
        } finally { writer.dispose(); }
        return output.toByteArray();
    }

    /** EXIF Orientation (tag 0x0112) from the first APP1 segment; 1 when absent or malformed. */
    static int exifOrientation(byte[] d) {
        try {
            int i = 2;
            while (i + 4 < d.length && (d[i] & 0xFF) == 0xFF) {
                int marker = d[i + 1] & 0xFF, len = ((d[i + 2] & 0xFF) << 8) | (d[i + 3] & 0xFF);
                if (marker == 0xDA || len < 2) return 1;
                if (marker == 0xE1 && i + 10 < d.length && d[i + 4] == 'E' && d[i + 5] == 'x' && d[i + 6] == 'i' && d[i + 7] == 'f') {
                    int t = i + 10; boolean le = d[t] == 'I';
                    java.util.function.IntUnaryOperator u16 = p -> le ? (d[p] & 0xFF) | (d[p + 1] & 0xFF) << 8 : (d[p] & 0xFF) << 8 | (d[p + 1] & 0xFF);
                    java.util.function.IntUnaryOperator u32 = p -> le ? (d[p] & 0xFF) | (d[p + 1] & 0xFF) << 8 | (d[p + 2] & 0xFF) << 16 | (d[p + 3] & 0xFF) << 24
                            : (d[p] & 0xFF) << 24 | (d[p + 1] & 0xFF) << 16 | (d[p + 2] & 0xFF) << 8 | (d[p + 3] & 0xFF);
                    int ifd = t + u32.applyAsInt(t + 4), entries = u16.applyAsInt(ifd);
                    for (int e = 0; e < entries && ifd + 2 + e * 12 + 10 <= d.length; e++) {
                        int at = ifd + 2 + e * 12;
                        if (u16.applyAsInt(at) == 0x0112) { int v = u16.applyAsInt(at + 8); return v >= 1 && v <= 8 ? v : 1; }
                    }
                    return 1;
                }
                i += 2 + len;
            }
        } catch (RuntimeException malformed) { /* treat as upright */ }
        return 1;
    }

    static BufferedImage orient(BufferedImage src, int orientation) {
        if (orientation <= 1) return src;
        int w = src.getWidth(), h = src.getHeight(); boolean swap = orientation >= 5;
        var t = new AffineTransform();
        switch (orientation) {
            case 2 -> { t.scale(-1, 1); t.translate(-w, 0); }
            case 3 -> { t.translate(w, h); t.rotate(Math.PI); }
            case 4 -> { t.scale(1, -1); t.translate(0, -h); }
            case 5 -> { t.rotate(Math.PI / 2); t.scale(1, -1); }
            case 6 -> { t.translate(h, 0); t.rotate(Math.PI / 2); }
            case 7 -> { t.scale(-1, 1); t.translate(-h, 0); t.translate(0, w); t.rotate(3 * Math.PI / 2); }
            case 8 -> { t.translate(0, w); t.rotate(3 * Math.PI / 2); }
            default -> { return src; }
        }
        var out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try { g.drawImage(src, t, null); } finally { g.dispose(); }
        return out;
    }
    private BusinessRuleException invalid(String message) { return new BusinessRuleException(message); }
}
