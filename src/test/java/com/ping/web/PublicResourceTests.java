package com.ping.web;

import com.ping.support.TestBrowser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("bootstrap")
class PublicResourceTests {
    @LocalServerPort int port;
    @Autowired ResourceUrlProvider urls;

    @Test void onlyValidatedPublicFingerprintsAreImmutable() throws Exception {
        String path = urls.getForLookupPath("/css/brand.css");
        assertThat(path).matches("/css/brand-[a-f0-9]{32}\\.css");
        try (var browser = new TestBrowser(port)) {
            var valid = browser.get(path);
            assertThat(valid.statusCode()).isEqualTo(200);
            assertThat(valid.headers().firstValue("Cache-Control")).hasValue("public, max-age=31536000, immutable");
            // CSS references must use the same fingerprint as the font preload.
            assertThat(valid.body()).contains(urls.getForLookupPath("/fonts/Manrope-Variable.ttf").substring(1));
            var plain = browser.get("/css/brand.css");
            assertThat(plain.statusCode()).isEqualTo(200);
            assertThat(plain.headers().firstValue("Cache-Control").orElse("")).doesNotContain("immutable");
            var invalid = browser.get(path.replaceFirst("[a-f0-9]{32}", "00000000000000000000000000000000"));
            assertThat(invalid.statusCode()).isEqualTo(404);
            assertThat(invalid.headers().firstValue("Cache-Control").orElse("")).doesNotContain("immutable");
            var privateLooking = browser.get("/me/state-00000000000000000000000000000000.json");
            assertThat(privateLooking.headers().firstValue("Cache-Control").orElse("")).doesNotContain("immutable");
        }
    }
}
