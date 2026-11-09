package com.ping.reward;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import com.ping.auth.AccountService;
import com.ping.event.*;
import com.ping.participant.*;
import com.ping.support.TestBrowser;
import com.ping.web.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

/** DEC-066: optional reward image validated and normalised on the server, stored apart from the list. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.datasource.url=jdbc:h2:mem:rewardimages;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("verification-test")
class BenefitImageTests {
    @Autowired EventService events; @Autowired BenefitAdminService admin; @Autowired BenefitImageService images; @Autowired BenefitRepository benefits;
    @Autowired ParticipantService people; @Autowired AccountService accounts; @Autowired JdbcTemplate sql; @Autowired PasswordEncoder encoder; @LocalServerPort int port;
    static final String PASSWORD = "Contraseña exclusiva de fixture2026";
    long event, other, benefit, foreign;

    @BeforeEach void setup() {
        event = event("Imágenes " + UUID.randomUUID()); other = event("Otro " + UUID.randomUUID());
        benefit = reward(event, "Café"); foreign = reward(other, "Camiseta");
    }
    long event(String name) { var f = new EventForm(); f.setName(name); f.setDeliveryName("Mesa"); f.setDeliveryDirections("Entrada"); long id = events.create(f).getId(); events.transition(id, EventStatus.OPEN); return id; }
    long reward(long event, String name) { var f = new BenefitForm(); f.setName(name); f.setDefaultDelivery(true); f.setPointsCost(20); return admin.save(event, null, f); }
    static byte[] encode(BufferedImage img, String format) throws IOException { var out = new ByteArrayOutputStream(); ImageIO.write(img, format, out); return out.toByteArray(); }
    static BufferedImage canvas(int w, int h) { var img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB); var g = img.createGraphics(); g.setColor(java.awt.Color.ORANGE); g.fillRect(0, 0, w, h); g.dispose(); return img; }
    /** JPEG with an EXIF APP1 (orientation 6 = rotate 90°) and a fake GPS-like string that must not survive. */
    static byte[] jpegWithExif(int w, int h) throws IOException {
        byte[] jpeg = encode(canvas(w, h), "jpeg");
        var tiff = new ByteArrayOutputStream();
        tiff.writeBytes(new byte[] {'I', 'I', 42, 0, 8, 0, 0, 0, 1, 0, 0x12, 0x01, 3, 0, 1, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0});
        tiff.writeBytes("GPS-FIXTURE-SECRET".getBytes());
        byte[] payload = tiff.toByteArray(); int len = payload.length + 8;
        var out = new ByteArrayOutputStream(); out.write(jpeg, 0, 2);
        out.writeBytes(new byte[] {(byte) 0xFF, (byte) 0xE1, (byte) (len >> 8), (byte) len, 'E', 'x', 'i', 'f', 0, 0}); out.writeBytes(payload);
        out.write(jpeg, 2, jpeg.length - 2); return out.toByteArray();
    }
    static MockMultipartFile file(String name, String type, byte[] bytes) { return new MockMultipartFile("image", name, type, bytes); }

    @Test void validRasterIsNormalisedOrientedAndStrippedOfMetadata() throws Exception {
        images.save(event, benefit, file("foto.jpg", "image/jpeg", jpegWithExif(400, 200)));
        var stored = images.read(benefit);
        assertThat(stored.contentType()).isEqualTo("image/jpeg");
        var decoded = ImageIO.read(new ByteArrayInputStream(stored.bytes()));
        assertThat(decoded.getWidth()).as("EXIF orientation 6 applied").isEqualTo(200);
        assertThat(decoded.getHeight()).isEqualTo(400);
        assertThat(new String(stored.bytes(), java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("Exif", "GPS-FIXTURE-SECRET");
        images.save(event, benefit, file("grande.png", "image/png", encode(canvas(3000, 1500), "png")));
        var big = ImageIO.read(new ByteArrayInputStream(images.read(benefit).bytes()));
        assertThat(Math.max(big.getWidth(), big.getHeight())).isEqualTo(BenefitImageService.MAX_SIDE);
        assertThat(images.read(benefit).bytes().length).isLessThanOrEqualTo(BenefitImageService.MAX_STORED);
    }

    @Test void nonRasterTinyOrDisguisedFilesAreRejectedWithoutChangingTheReward() throws Exception {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='400' height='400'><script>alert(1)</script></svg>";
        assertThatThrownBy(() -> images.save(event, benefit, file("x.svg", "image/svg+xml", svg.getBytes()))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> images.save(event, benefit, file("x.png", "image/png", "not an image".getBytes()))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> images.save(event, benefit, file("tiny.png", "image/png", encode(canvas(40, 40), "png")))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> images.save(event, benefit, file("empty.png", "image/png", new byte[0]))).isInstanceOf(BusinessRuleException.class);
        assertThat(benefits.findById(benefit).orElseThrow().hasImage()).isFalse();
        assertThat(sql.queryForObject("select count(*) from benefit_images where benefit_id=?", Integer.class, benefit)).isZero();
    }

    @Test void replaceAndRemoveKeepTheRewardAndChangeTheRevision() throws Exception {
        images.save(event, benefit, file("a.png", "image/png", encode(canvas(300, 300), "png")));
        long first = benefits.findById(benefit).orElseThrow().getImageRevision();
        images.save(event, benefit, file("b.png", "image/png", encode(canvas(320, 300), "png")));
        long second = benefits.findById(benefit).orElseThrow().getImageRevision();
        assertThat(second).isGreaterThan(first);
        assertThat(sql.queryForObject("select count(*) from benefit_images where benefit_id=?", Integer.class, benefit)).isOne();
        images.remove(event, benefit);
        var kept = benefits.findById(benefit).orElseThrow();
        assertThat(kept.hasImage()).isFalse(); assertThat(kept.getName()).isEqualTo("Café"); assertThat(kept.isActive()).isTrue();
        assertThatThrownBy(() -> images.read(benefit)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> images.save(other, benefit, file("a.png", "image/png", encode(canvas(300, 300), "png")))).isInstanceOf(NotFoundException.class);
    }

    @Test void participantsOnlyGetImagesOfTheirOwnEventAndStaffNeedPermission() throws Exception {
        images.save(event, benefit, file("a.png", "image/png", encode(canvas(300, 300), "png")));
        images.save(other, foreign, file("b.png", "image/png", encode(canvas(300, 300), "png")));
        String email = UUID.randomUUID() + "@fixture.test";
        sql.update("insert into ping_accounts(email,password_hash,email_verified) values(?,?,true)", email, encoder.encode(PASSWORD));
        long account = accounts.find(email).orElseThrow();
        var f = new ParticipantForm(); f.setAlias("Persona"); f.setEventId(event); f.setAvailable(true); long self = people.create(f).id(); accounts.bind(account, self, event);
        try (var browser = new TestBrowser(port)) {
            browser.get("/login"); assertThat(browser.post("/login", Map.of("email", email, "password", PASSWORD)).statusCode()).isEqualTo(302);
            browser.get("/me");
            String page = browser.get("/me/benefits").body();
            assertThat(page).contains("/me/benefits/" + benefit + "/image?v=").doesNotContain("Camiseta", "Snack del evento");
            var own = browser.get("/me/benefits/" + benefit + "/image?v=1");
            assertThat(own.statusCode()).isEqualTo(200);
            assertThat(own.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
            assertThat(browser.get("/me/benefits/" + foreign + "/image").statusCode()).isEqualTo(404);
            assertThat(browser.get("/events/" + event + "/rewards/" + benefit + "/image").statusCode()).isEqualTo(403);
        }
    }
}
