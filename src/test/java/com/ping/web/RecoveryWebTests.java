package com.ping.web;

import com.ping.event.*;
import com.ping.participant.*;
import com.ping.match.*;
import com.ping.support.TestBrowser;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="spring.datasource.url=jdbc:h2:mem:recovery-web;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class RecoveryWebTests {
    @LocalServerPort int port;
    @Autowired EventService events;
    @Autowired ParticipantService participants;
    @Autowired MatchingService matching;
    @Autowired MatchService matches;
    long event;
    @BeforeEach void setup() { var form=new EventForm();form.setName("Evento ficticio recuperación");event=events.create(form).getId();events.transition(event,EventStatus.OPEN); }
    long person(String alias) {var f=new ParticipantForm();f.setAlias(alias);f.setEventId(event);return participants.create(f).id();}
    void enter(TestBrowser b,long id) throws Exception {b.get("/demo");b.post("/demo",Map.of("participantId",""+id));}
    @Test void absentSessionReturnsPrivateJson401AndHtmlEntryWithoutDemoAdoption() throws Exception {
        try(var b=new TestBrowser(port)) {
            for(String path:List.of("/me/state","/matches/42/recognition-hint")) {
                var r=b.get(path);assertThat(r.statusCode()).isEqualTo(401);
                assertThat(r.headers().firstValue("Content-Type").orElse("")).contains("application/json");
                assertThat(r.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
                assertThat(r.body()).contains("SESSION_REQUIRED","/join").doesNotContain("<html","signature","recognitionRevision");
            }
            var r=b.get("/matches/42");assertThat(r.statusCode()).isEqualTo(401);
            assertThat(r.body()).contains("no recupera tu identidad", "href=\"/join\"").doesNotContain("Seleccionar participante");
        }
    }
    @Test void missingResourceAndCsrfKeepHttpCodesAndOfferSessionAppropriateRecovery() throws Exception {
        try(var b=new TestBrowser(port)) {
            var missing=b.get("/un-enlace-que-no-existe");assertThat(missing.statusCode()).isEqualTo(404);
            assertThat(missing.body()).contains("href=\"/join\"").doesNotContain("NoResourceFoundException","static resource","Seleccionar participante");
            var expired=b.post("/me/search",Map.of());assertThat(expired.statusCode()).isEqualTo(403);
            assertThat(expired.body()).contains("No se guardaron cambios", "no podemos recuperar aquí tu identidad");
            enter(b,person("Propietaria"));missing=b.get("/matches/99999999");assertThat(missing.statusCode()).isEqualTo(404);
            assertThat(missing.body()).contains("href=\"/connect\"", "Ir a Conectar");
            var method=b.get("/me/search");assertThat(method.statusCode()).isEqualTo(405);
        }
    }
    @Test void cancelledProposalCanBeReviewedWithoutAcceptingOrAwardingPoints() throws Exception {
        long first=person("Primera"),second=person("Segunda");
        for(long id:List.of(first,second)){var f=new AvailabilityForm();f.setVersion(participants.get(id).version());f.setAvailable(true);participants.setAvailability(id,f);}
        long match=matching.search(first);matches.cancel(match,second);
        try(var b=new TestBrowser(port)) {
            enter(b,first);var r=b.post("/matches/"+match+"/accept",Map.of());assertThat(r.statusCode()).isEqualTo(409);
            assertThat(r.body()).contains("ya se canceló", "href=\"/matches/"+match+"\"");
            assertThat(matches.get(match,first).status()).isEqualTo(MatchStatus.CANCELLED);
            assertThat(participants.get(first).points()).isZero();assertThat(participants.get(second).points()).isZero();
        }
    }
}
