package com.ping.demo;

import com.ping.event.*;
import com.ping.match.*;
import com.ping.participant.*;
import com.ping.support.TestBrowser;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:recognition-hints;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class RecognitionHintTests {
    @Autowired ParticipantService participants;
    @Autowired RecognitionHintService hints;
    @Autowired EventService events;
    @Autowired MatchingService matching;
    @Autowired MatchService matches;
    @Autowired JdbcTemplate sql;
    @LocalServerPort int port;
    long a,b,c,event;
    @BeforeEach void setup() {
        // H2 VARCHAR counts UTF-16 units; production MySQL counts code points.
        // Allow surrogate pairs here while the application still enforces 120 code points.
        sql.execute("alter table participants alter column recognition_hint varchar(240)");
        EventForm f=new EventForm(); f.setName("Evento privado "+UUID.randomUUID());
        event=events.create(f).getId(); events.transition(event,EventStatus.OPEN);
        a=person("Uno"); b=person("Dos"); c=person("Tres");
    }
    long person(String alias) { ParticipantForm f=new ParticipantForm(); f.setEventId(event); f.setAlias(alias); return participants.create(f).id(); }
    void enter(TestBrowser browser,long id) throws Exception { browser.get("/demo"); assertThat(browser.post("/demo",Map.of("participantId",""+id)).statusCode()).isEqualTo(302); browser.get("/me"); }
    Map<String,String> fields(long self,String hint) { return Map.of("hint",hint,"version",""+participants.get(self).version()); }
    void save(long id,String hint) { var f=hints.ownForm(id); f.setHint(hint); hints.save(id,f); }
    void available(long id) { var f=new AvailabilityForm(); f.setAvailable(true); f.setVersion(participants.get(id).version()); participants.setAvailability(id,f); }
    long offer() { available(a); available(b); return matching.search(a); }
    @Test void saveChangesOnlyHintAndVersionAndBlankRemovesIt() {
        available(a);
        var before=sql.queryForMap("select * from participants where id=?",a);
        save(a,"  Morral violeta  "); var after=sql.queryForMap("select * from participants where id=?",a);
        assertThat(after.get("RECOGNITION_HINT")).isEqualTo("Morral violeta");
        before.remove("VERSION"); before.remove("RECOGNITION_HINT"); after.remove("VERSION"); after.remove("RECOGNITION_HINT");
        assertThat(after).isEqualTo(before); assertThat(participants.get(a).points()).isZero();
        save(a," \n\t "); assertThat(hints.ownForm(a).getHint()).isEmpty();
        assertThat(sql.queryForObject("select recognition_hint from participants where id=?",String.class,a)).isNull();
    }
    @Test void unicodeLimitAllows120CodePointsAndRejects121WithoutTruncating() throws Exception {
        try(var browser=new TestBrowser(port)) {
            enter(browser,a); String exact="🟣".repeat(120);
            assertThat(browser.post("/me/recognition-hint",fields(a,exact)).statusCode()).isEqualTo(302);
            assertThat(hints.ownForm(a).getHint()).isEqualTo(exact);
            var rejected=browser.post("/me/recognition-hint",fields(a,exact+"🟣"));
            assertThat(rejected.statusCode()).isEqualTo(400); assertThat(rejected.body()).contains("120 caracteres",exact+"🟣");
            assertThat(hints.ownForm(a).getHint()).isEqualTo(exact);
            assertThatThrownBy(() -> save(a,"x".repeat(121))).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        }
    }
    @Test void staleSubmissionPreservesDraftAndDoesNotOverwritePointsOrAvailability() throws Exception {
        try(var browser=new TestBrowser(port)) {
            enter(browser,a); var draft=fields(a,"Borrador que conservar"); available(a);
            var stale=browser.post("/me/recognition-hint",draft);
            assertThat(stale.statusCode()).isEqualTo(409); assertThat(stale.body()).contains("Borrador que conservar","Tu información cambió");
            assertThat(hints.ownForm(a).getHint()).isEmpty(); assertThat(participants.get(a).available()).isTrue(); assertThat(participants.get(a).points()).isZero();
            assertThat(browser.post("/me/recognition-hint",fields(a,"Borrador que conservar")).statusCode()).isEqualTo(302);
        }
    }
    @Test void sessionOwnsUpdatesAndCsrfIsRequired() throws Exception {
        try(var browser=new TestBrowser(port); var noCsrf=new TestBrowser(port)) {
            enter(browser,a); var form=new HashMap<>(fields(a,"Solo mío")); form.put("participantId",""+b);
            assertThat(browser.post("/me/recognition-hint",form).statusCode()).isEqualTo(302);
            assertThat(hints.ownForm(a).getHint()).isEqualTo("Solo mío"); assertThat(hints.ownForm(b).getHint()).isEmpty();
            assertThat(noCsrf.post("/me/recognition-hint",fields(a,"Intruso")).statusCode()).isEqualTo(403);
            assertThat(hints.ownForm(a).getHint()).isEqualTo("Solo mío");
        }
    }
    @Test void counterpartIsAuthorizedOnlyAfterBothAcceptAndDisappearsAfterCompletion() throws Exception {
        save(b,"SECRETO-PRUEBA"); long m=offer();
        try(var owner=new TestBrowser(port); var outside=new TestBrowser(port)) {
            enter(owner,a); enter(outside,c); String path="/matches/"+m+"/recognition-hint";
            assertThat(owner.get(path).body()).contains("\"active\":false").doesNotContain("SECRETO-PRUEBA","revision");
            matches.accept(m,a); assertThat(owner.get(path).body()).doesNotContain("SECRETO-PRUEBA");
            matches.accept(m,b); var active=owner.get(path);
            assertThat(active.statusCode()).isEqualTo(200); assertThat(active.body()).contains("SECRETO-PRUEBA","revision");
            assertThat(active.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
            assertThat(outside.get(path).statusCode()).isEqualTo(404); assertThat(outside.get(path).body()).doesNotContain("SECRETO-PRUEBA");
            assertThat(owner.get("/me/state").body()).contains("recognitionRevision").doesNotContain("SECRETO-PRUEBA");
            matches.confirm(m,a); matches.confirm(m,b);
            assertThat(owner.get(path).body()).doesNotContain("SECRETO-PRUEBA","revision");
            assertThat(owner.get("/me/state").body()).doesNotContain("SECRETO-PRUEBA","recognitionRevision");
            assertThat(owner.get("/matches/"+m).body()).doesNotContain("SECRETO-PRUEBA");
            assertThat(participants.get(a).points()).isEqualTo(10);
        }
    }
    @Test void plainTextIsEscapedAndNeverAddedToPublicViewsOrAnotherOwnersEditor() throws Exception {
        String unsafe="<script>HINT_PRIVATE</script>"; save(b,unsafe); long m=offer(); matches.accept(m,a); matches.accept(m,b);
        try(var owner=new TestBrowser(port)) {
            enter(owner,a);
            assertThat(owner.get("/matches/"+m).body()).contains("&lt;script&gt;HINT_PRIVATE&lt;/script&gt;").doesNotContain(unsafe);
            for(String path:List.of("/participants","/participants/"+b,"/admin?eventId="+event,"/me"))
                assertThat(owner.get(path).body()).doesNotContain("HINT_PRIVATE");
            matches.cancel(m,a); assertThat(owner.get("/matches/"+m+"/recognition-hint").body()).doesNotContain("HINT_PRIVATE");
            assertThat(owner.get("/me").body()).doesNotContain("HINT_PRIVATE");
        }
        assertThat(Arrays.stream(ParticipantView.class.getRecordComponents()).map(x->x.getName())).doesNotContain("recognitionHint");
    }
    @Test void updatingDuringAnActiveMatchKeepsItsReservationAndRevisionChanges() {
        long m=offer(); matches.accept(m,a); matches.accept(m,b); var revision=hints.other(m,a).revision();
        save(b,"Sombrero verde"); assertThat(hints.other(m,a).revision()).isNotEqualTo(revision);
        assertThat(hints.other(m,a).text()).isEqualTo("Sombrero verde");
        assertThat(matching.search(a)).isEqualTo(m); assertThat(matches.get(m,a).status()).isEqualTo(MatchStatus.ACTIVE);
        assertThat(participants.get(b).points()).isZero();
    }
}
