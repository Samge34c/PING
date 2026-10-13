package com.ping;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("bootstrap")
class PingApplicationTests {

    @Autowired
    private ApplicationContext context;

    @LocalServerPort
    private int port;

    @Test
    void loadsWebContextWithoutPersistenceInBootstrapProfile() {
        assertThat(context.containsBean("dispatcherServlet")).isTrue();
        assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
    }

    @Test
    void servesHttpWithoutBusinessEndpoints() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + "/"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(404);
            assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                    contentType -> assertThat(contentType).contains("application/json"));
            assertThat(response.body()).contains("Not Found");
        }
    }
}
