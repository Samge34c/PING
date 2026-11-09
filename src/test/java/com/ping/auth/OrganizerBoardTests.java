package com.ping.auth;

import java.util.*;
import com.ping.event.*;
import com.ping.support.TestBrowser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

/** DEC-066: the organisation page shows one permitted event at a time and keeps existing permissions. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"spring.datasource.url=jdbc:h2:mem:organizer;MODE=MySQL;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=validate"})
@ActiveProfiles("verification-test")
class OrganizerBoardTests {
    @Autowired EventService events; @Autowired AccountService accounts; @Autowired JdbcTemplate sql; @Autowired PasswordEncoder encoder; @LocalServerPort int port;
    static final String PASSWORD = "Contraseña exclusiva de fixture2026";
    long account() { String email = UUID.randomUUID() + "@fixture.test"; sql.update("insert into ping_accounts(email,password_hash,email_verified) values(?,?,true)", email, encoder.encode(PASSWORD)); return accounts.find(email).orElseThrow(); }
    long general() { String email = "general@fixture.test"; if (accounts.find(email).isEmpty()) accounts.bootstrap(email, PASSWORD); return accounts.find(email).orElseThrow(); }
    long event(String name) { var f = new EventForm(); f.setName(name); return events.create(f).getId(); }

    @Test void staffSeeOnlyTheirEventAndLifecycleReturnsToTheBoard() throws Exception {
        long mine = event("Feria propia " + UUID.randomUUID()), foreign = event("Feria ajena " + UUID.randomUUID());
        long staff = account(); accounts.assign(general(), staff, mine, true);
        try (var b = new TestBrowser(port)) {
            b.get("/login");
            var signedIn = b.post("/login", Map.of("email", accounts.user(staff).email(), "password", PASSWORD));
            assertThat(signedIn.statusCode()).isEqualTo(302);
            assertThat(signedIn.headers().firstValue("Location").orElse("")).as("organising-only account lands on its board").endsWith("/organizer");
            String page = b.get("/organizer").body();
            assertThat(page).contains("Evento que administras", "Validaciones y entregas", "Preparar evento", "Abrir evento", "/events/" + mine + "/reviews")
                .doesNotContain("Feria ajena", "Cambiar de evento", "/organizer/access\"");
            assertThat(b.get("/organizer?event=" + foreign).statusCode()).isEqualTo(403);
            var opened = b.post("/events/" + mine + "/open", Map.of("returnTo", "organizer"));
            assertThat(opened.statusCode()).isEqualTo(302);
            assertThat(opened.headers().firstValue("Location").orElse("")).endsWith("/organizer?event=" + mine);
            assertThat(events.get(mine).getStatus()).isEqualTo(EventStatus.OPEN);
            var other = b.post("/events/" + mine + "/pause", Map.of("returnTo", "https://example.invalid/"));
            assertThat(other.headers().firstValue("Location").orElse("")).endsWith("/events/" + mine);
            assertThat(b.post("/events/" + foreign + "/open", Map.of("returnTo", "organizer")).statusCode()).isEqualTo(403);
        }
    }
}
