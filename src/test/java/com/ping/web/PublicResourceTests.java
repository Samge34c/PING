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

    @Test void gzipPreservesAssetBytesAndVariesOnEncoding() throws Exception {
        String path = urls.getForLookupPath("/css/brand.css");
        try (var client = java.net.http.HttpClient.newHttpClient(); var browser = new TestBrowser(port)) {
            var zipped = client.send(java.net.http.HttpRequest.newBuilder(
                    java.net.URI.create("http://127.0.0.1:" + port + path))
                    .header("Accept-Encoding", "gzip").GET().build(), java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            assertThat(zipped.statusCode()).isEqualTo(200);
            assertThat(zipped.headers().firstValue("Content-Encoding")).hasValue("gzip");
            assertThat(String.join(",", zipped.headers().allValues("Vary")).toLowerCase()).contains("accept-encoding");
            try (var decoded = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(zipped.body()))) {
                byte[] plain = browser.bytes(path).body();
                assertThat(decoded.readAllBytes()).isEqualTo(plain);
                assertThat(zipped.body().length).isLessThan(plain.length);
            }
        }
    }

    @Test void onlyValidatedPublicFingerprintsAreImmutable() throws Exception {
        String path = urls.getForLookupPath("/css/brand.css");
        assertThat(path).matches("/css/brand-[a-f0-9]{32}\\.css");
        try (var browser = new TestBrowser(port)) {
            var valid = browser.get(path);
            assertThat(valid.statusCode()).isEqualTo(200);
            assertThat(valid.headers().firstValue("Cache-Control")).hasValue("public, max-age=31536000, immutable");
            // CSS references must use the same fingerprint as the font preload.
            assertThat(valid.body()).contains(urls.getForLookupPath("/fonts/Manrope-Variable-core.ttf").substring(1));
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
