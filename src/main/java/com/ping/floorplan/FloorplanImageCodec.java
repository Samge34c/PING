package com.ping.floorplan;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;
import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.ping.web.BusinessRuleException;

@Component
public class FloorplanImageCodec {
    public static final int MAX_INPUT = 8 * 1024 * 1024;
    public static final int MAX_STORED = 4 * 1024 * 1024;
    public record Image(String filename, int width, int height, String contentType, byte[] bytes) { }

    public Image decode(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("Elige una imagen PNG, JPEG o WebP.");
        if (file.getSize() > MAX_INPUT) throw invalid("La imagen supera 8 MiB. Elige una versión más ligera.");
        try (var input = file.getInputStream()) {
            byte[] data = input.readNBytes(MAX_INPUT + 1);
            if (data.length > MAX_INPUT) throw invalid("La imagen supera 8 MiB.");
            try (var stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(data))) {
                var readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw invalid("No pudimos leer esa imagen. Usa PNG, JPEG o WebP.");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(stream);
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!java.util.Set.of("png", "jpeg", "jpg", "webp").contains(format)) throw invalid("Solo se admiten PNG, JPEG o WebP; no SVG ni otros formatos.");
                    int w = reader.getWidth(0), h = reader.getHeight(0);
                    if (w < 32 || h < 32 || w > 6000 || h > 6000 || (long) w * h > 16_000_000) throw invalid("Usa una imagen de al menos 32 px, hasta 6000 px por lado y 16 megapíxeles.");
                    if (reader.getNumImages(true) != 1 || animated(data, format)) throw invalid("Usa un plano de imagen fija, sin animación.");
                    BufferedImage original = reader.read(0);
                    if (original == null) throw invalid("La imagen está dañada. Vuelve a exportarla.");
                    double factor = Math.min(1, 3200d / Math.max(w, h));
                    int width = Math.max(1, (int) Math.round(w * factor)), height = Math.max(1, (int) Math.round(h * factor));
                    BufferedImage normalized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = normalized.createGraphics();
                    try {
                        graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, width, height);
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        graphics.drawImage(original, 0, 0, width, height, null);
                    } finally { graphics.dispose(); original.flush(); }
                    try {
                        var output = new ByteArrayOutputStream(); ImageIO.write(normalized, "png", output);
                        String type = "image/png";
                        if (output.size() > MAX_STORED) {
                            output.reset(); var writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                            try (var target = new MemoryCacheImageOutputStream(output)) {
                                writer.setOutput(target); var options = writer.getDefaultWriteParam();
                                options.setCompressionMode(ImageWriteParam.MODE_EXPLICIT); options.setCompressionQuality(.90f);
                                writer.write(null, new IIOImage(normalized, null, null), options);
                            } finally { writer.dispose(); }
                            type = "image/jpeg";
                        }
                        if (output.size() > MAX_STORED) throw invalid("El plano sigue siendo demasiado grande. Exporta una versión de hasta 3200 px manteniendo legibles sus etiquetas.");
                        String name = java.util.Objects.requireNonNullElse(file.getOriginalFilename(), "Plano").replace('\\', '/');
                        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").strip();
                        if (name.isEmpty()) name = "Plano";
                        return new Image(name.substring(0, Math.min(160, name.length())), width, height, type, output.toByteArray());
                    } finally { normalized.flush(); }
                } finally { reader.dispose(); }
            }
        } catch (IOException | IllegalArgumentException e) {
            throw invalid("La imagen no se pudo decodificar. Vuelve a exportarla como PNG, JPEG o WebP.");
        }
    }

    private boolean animated(byte[] data, String format) {
        if (format.equals("webp")) return data.length > 20 && data[12] == 'V' && data[15] == 'X' && (data[20] & 2) != 0;
        if (!format.equals("png")) return false;
        for (int offset = 8; offset + 12 <= data.length;) {
            int length = java.nio.ByteBuffer.wrap(data, offset, 4).getInt();
            if (length < 0 || (long) offset + length + 12 > data.length) return false;
            if (data[offset + 4] == 'a' && data[offset + 5] == 'c' && data[offset + 6] == 'T' && data[offset + 7] == 'L') return true;
            offset += length + 12;
        }
        return false;
    }
    private BusinessRuleException invalid(String message) { return new BusinessRuleException(message); }
}
