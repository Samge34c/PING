package com.ping.event;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.imageio.ImageIO;
import com.google.zxing.*;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.ping.web.BusinessRuleException;

/** Explicit deployment address: never infer it from Host/Forwarded or a session. */
@Component @Profile("!bootstrap")
public class EventEntryLinks {
    private final String base;
    public EventEntryLinks(@Value("${ping.public-base-url:}") String configured) {
        base = configured.strip().replaceAll("/+$", "");
        if (!base.isEmpty()) {
            URI uri = URI.create(base);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("PING_PUBLIC_BASE_URL debe ser una dirección HTTP(S) absoluta, sin credenciales, consulta ni fragmento.");
            }
        }
    }
    public String baseUrl() { return base.isEmpty() ? null : base; }
    public String link(Event event) { return base.isEmpty() ? null : base + "/join?code=" + event.getEntryCode(); }
    public byte[] qr(Event event) {
        String link = link(event);
        if (link == null) throw new BusinessRuleException("Configura PING_PUBLIC_BASE_URL con la dirección accesible de PING antes de generar el QR.");
        try {
            var matrix = new QRCodeWriter().encode(link, BarcodeFormat.QR_CODE, 384, 384,
                    Map.of(EncodeHintType.MARGIN, 4, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M));
            var image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < matrix.getHeight(); y++) for (int x = 0; x < matrix.getWidth(); x++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xffffff);
            }
            var output = new ByteArrayOutputStream(); ImageIO.write(image, "PNG", output); return output.toByteArray();
        } catch (WriterException | java.io.IOException error) { throw new IllegalStateException("No pudimos generar el QR local.", error); }
    }
}
