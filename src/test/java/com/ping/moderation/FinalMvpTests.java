package com.ping.moderation;

import com.ping.admin.AdminService;
import com.ping.demo.DemoPreparationService;
import com.ping.event.*;
import com.ping.match.*;
import com.ping.participant.*;
import com.ping.support.TestBrowser;
import com.ping.web.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:final-mvp;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class FinalMvpTests {
    @Autowired DemoPreparationService preparation;
    @Autowired ParticipantService participants;
    @Autowired MatchingService matching;
    @Autowired MatchService matches;
    @Autowired ModerationService moderation;
    @Autowired BlockRepository blocks;
    @Autowired ReportRepository reports;
    @Autowired AdminService admin;
    @Autowired EventService events;
    @Autowired JdbcTemplate sql;
    @LocalServerPort int port;
    long a, b, c, event;
    @BeforeEach void prepare() {
        sql.update("update demo_scenarios set event_id = null");
        sql.update("delete from interaction_reports"); sql.update("delete from participant_blocks");
        sql.update("delete from feedback"); sql.update("delete from match_reservations"); sql.update("delete from ping_matches");
        sql.update("delete from participant_interests"); sql.update("delete from participants"); sql.update("delete from events");
        var s = preparation.prepare(); event = s.eventId(); a = id(s,"Samuel"); b = id(s,"Julián"); c = id(s,"Ana");
    }
    long id(DemoPreparationService.Scenario s, String alias) { return s.people().stream().filter(p -> p.alias().equals(alias)).findFirst().orElseThrow().id(); }
    void available(long id, boolean value) {
        AvailabilityForm f = new AvailabilityForm(); f.setAvailable(value); f.setVersion(participants.get(id).version()); participants.setAvailability(id,f);
    }
    long offer() { available(a,true); available(b,true); return matching.search(a); }
    long complete() { long m=offer(); matches.accept(m,a); matches.accept(m,b); matches.arrive(m,a); matches.arrive(m,b); matches.confirm(m,a); matches.confirm(m,b); return m; }
    void cycle(long id) { available(id,false); available(id,true); }
    @Test void blockExcludesBothDirectionsAcrossAvailabilityCyclesAndPreservesHistory() {
        long m=complete(); moderation.block(m,a); moderation.block(m,a);
        cycle(a); cycle(b);
        assertThat(blocks.count()).isEqualTo(1);
        assertThatThrownBy(() -> matching.search(a)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> matching.search(b)).isInstanceOf(BusinessRuleException.class);
        available(c,true); long other = matching.search(b);
        assertThat(matches.get(other,b).otherAlias()).isEqualTo("Ana");
        assertThat(matches.get(other,b).commonInterests()).isEmpty();
        assertThat(matches.list(a)).singleElement().satisfies(v -> { assertThat(v.status()).isEqualTo(MatchStatus.COMPLETED); assertThat(v.activity()).isNotBlank(); });
        assertThat(participants.get(a).points()).isEqualTo(10); assertThat(moderation.status(m,a).blocked()).isTrue();
    }
    @Test void blockingCurrentOfferDoesNotRewriteOrCancelIt() {
        long m=offer(); moderation.block(m,a);
        assertThat(matches.get(m,a).status()).isEqualTo(MatchStatus.OFFERED);
        assertThat(matching.search(b)).isEqualTo(m);
        matches.accept(m,a); matches.accept(m,b); matches.arrive(m,a); matches.arrive(m,b); matches.confirm(m,a); matches.confirm(m,b);
        assertThat(participants.get(a).points()).isEqualTo(10);
        assertThatThrownBy(() -> matching.search(a)).isInstanceOf(BusinessRuleException.class);
    }
    @Test void reportIsIdempotentAndNeverBlocksOrChangesPoints() {
        long m=complete(); moderation.report(m,a,ReportReason.UNCOMFORTABLE,"  Comentario breve  ");
        moderation.report(m,a,ReportReason.OTHER,"Segundo envío");
        assertThat(reports.count()).isEqualTo(1); assertThat(blocks.count()).isZero();
        assertThat(moderation.forEvent(event)).singleElement().satisfies(v -> { assertThat(v.reporter()).isEqualTo("Samuel"); assertThat(v.other()).isEqualTo("Julián"); assertThat(v.comment()).isEqualTo("Comentario breve"); assertThat(v.createdAt()).isNotNull(); });
        assertThat(participants.get(a).points()).isEqualTo(10); assertThat(matches.get(m,a).status()).isEqualTo(MatchStatus.COMPLETED);
        // Reporting adds no block; the independent completed-pair rule excludes this pair.
        assertThatThrownBy(()->matching.search(a)).isInstanceOf(BusinessRuleException.class);
    }
    @Test void reportRejectsActiveInteractionInvalidReasonAndOversizedComment() {
        long m=offer(); matches.accept(m,a); matches.accept(m,b);
        assertThatThrownBy(() -> moderation.report(m,a,ReportReason.NO_SHOW,"test")).isInstanceOf(BusinessRuleException.class);
        matches.cancel(m,b);
        assertThatThrownBy(() -> moderation.report(m,a,null,"")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> moderation.report(m,a,ReportReason.OTHER,"x".repeat(301))).isInstanceOf(BusinessRuleException.class);
        moderation.report(m,a,ReportReason.NO_SHOW,""); assertThat(reports.count()).isEqualTo(1);
    }
    @Test void outsiderCannotBlockReportOrReadModerationStatus() {
        long m=complete();
        assertThatThrownBy(() -> moderation.block(m,c)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> moderation.report(m,c,ReportReason.OTHER,"")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> moderation.status(m,c)).isInstanceOf(NotFoundException.class);
        assertThat(blocks.count()).isZero(); assertThat(reports.count()).isZero();
    }
    @Test void concurrentBlocksAndReportsOnlyCreateOneRecordPerAuthor() throws Exception {
        long m=complete();
        try(var pool=Executors.newFixedThreadPool(4)) {
            var start=new CountDownLatch(1); List<Future<?>> jobs=new ArrayList<>();
            for(int i=0;i<4;i++) jobs.add(pool.submit(() -> { try { start.await(); moderation.block(m,a); moderation.report(m,a,ReportReason.OTHER,""); } catch(InterruptedException e) { throw new RuntimeException(e); } }));
            start.countDown(); for(var job:jobs) job.get(15,TimeUnit.SECONDS);
        }
        assertThat(blocks.count()).isEqualTo(1); assertThat(reports.count()).isEqualTo(1);
    }
    @Test void databaseConstraintsRejectDuplicateBlocksAndReports() {
        long m=complete(); moderation.block(m,a); moderation.report(m,a,ReportReason.OTHER,"");
        assertThatThrownBy(() -> sql.update("insert into participant_blocks(blocker_id,blocked_id,created_at) values(?,?,CURRENT_TIMESTAMP)",a,b)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> sql.update("insert into interaction_reports(match_id,reporter_id,reason,comment,created_at) values(?,?,'OTHER','',CURRENT_TIMESTAMP)",m,a)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void fullFlowThroughTwoHttpSessionsShowsHistoryAdminAndEscapesReportComment() throws Exception {
        try(var sam=new TestBrowser(port); var jul=new TestBrowser(port)) {
            sam.get("/demo"); jul.get("/demo");
            assertThat(sam.post("/demo",Map.of("participantId",""+a)).statusCode()).isEqualTo(302);
            jul.post("/demo",Map.of("participantId",""+b)); sam.get("/me"); jul.get("/me");
            assertThat(sam.post("/me/availability",Map.of("available","true","version",""+participants.get(a).version())).statusCode()).isEqualTo(302);
            jul.post("/me/availability",Map.of("available","true","version",""+participants.get(b).version()));
            var search=sam.post("/me/search",Map.of()); String path=search.headers().firstValue("Location").orElseThrow();
            assertThat(sam.get(path).body()).contains("Videojuegos").doesNotContain("Zona Gamer · Pabellón 18");
            sam.post(path+"/accept",Map.of()); assertThat(sam.get(path).body()).contains("Esperando respuesta de Julián");
            assertThat(jul.get("/me").body()).contains("Ver propuesta");
            assertThat(jul.get("/connect").headers().firstValue("Location").orElseThrow()).endsWith(java.net.URI.create(path).getPath());
            assertThat(jul.get(path).body()).contains("Samuel aceptó. Falta tu aceptación.");
            jul.get(path); jul.post(path+"/accept",Map.of());
            assertThat(sam.get(path).body()).contains("Zona Gamer · Pabellón 18", "PIXEL-", "Elijan un juego o stand");
            assertThat(sam.get("/me").body()).contains("Tu encuentro continúa.", "Volver al encuentro").doesNotContain("PIXEL-");
            sam.post(path+"/arrive",Map.of()); jul.post(path+"/arrive",Map.of());
            sam.post(path+"/confirm",Map.of()); assertThat(sam.get(path).body()).contains("Esperando a Julián");
            assertThat(jul.get(path).body()).contains("Samuel ya confirmó. Falta tu confirmación.");
            jul.post(path+"/confirm",Map.of()); assertThat(sam.get(path).body()).contains("Conexión completada 🎉");
            sam.post(path+"/feedback",Map.of("rating","USEFUL"));
            sam.post(path+"/report",Map.of("reason","OTHER","comment","<script>alert(1)</script>"));
            assertThat(sam.get("/me").body()).contains("Feedback guardado", "Elijan un juego o stand", "+10 puntos acreditados");
            String panel=sam.get("/admin?eventId="+event).body();
            assertThat(panel).contains("Samuel", "Julián", "Positivo", "100%", "&lt;script&gt;").doesNotContain("<script>alert(1)</script>");
            assertThat(participants.get(a).points()).isEqualTo(10); assertThat(participants.get(b).points()).isEqualTo(10);
            assertThat(jul.post(path+"/confirm",Map.of()).statusCode()).isEqualTo(302); assertThat(participants.get(b).points()).isEqualTo(10);
        }
    }
    @Test void moderationWebRequiresTokenAndSessionAndValidInputs() throws Exception {
        long m=complete(); String path="/matches/"+m;
        try(var browser=new TestBrowser(port)) {
            assertThat(browser.post(path+"/block",Map.of()).statusCode()).isEqualTo(403);
            browser.get("/demo"); assertThat(browser.post(path+"/block",Map.of()).statusCode()).isEqualTo(401);
            browser.post("/demo",Map.of("participantId",""+a)); browser.get(path);
            assertThat(browser.post(path+"/report",Map.of("reason","INVALID")).statusCode()).isEqualTo(400);
            assertThat(browser.post(path+"/report",Map.of("reason","OTHER","comment","x".repeat(301))).statusCode()).isEqualTo(409);
            assertThat(reports.count()).isZero();
        }
    }
    @Test void reportAdminQueryDoesNotMixEventsAndFeedbackOmissionIsNotNeutral() {
        long m=complete(); moderation.report(m,b,ReportReason.INAPPROPRIATE,"");
        EventForm f=new EventForm(); f.setName("Otro evento"); long other=events.create(f).getId();
        assertThat(moderation.forEvent(other)).isEmpty(); assertThat(admin.details(other).feedback()).isEmpty();
        var metrics=admin.metrics().stream().filter(v -> v.id()==event).findFirst().orElseThrow();
        assertThat(metrics.neutral()).isZero(); assertThat(metrics.feedbackCount()).isZero(); assertThat(metrics.completionPercent()).isEqualTo(100);
    }
    @Test void demoEntryPreparationAndParticipantNavigationAreDirect() throws Exception {
        try(var browser=new TestBrowser(port)) {
            String entry=browser.get("/demo").body();
            assertThat(entry).contains("Preparar demo rápida", "href=\"/demo/prepare\"").doesNotContain("href=\"/me/benefits\"");
            assertThat(browser.get("/participants/"+a).body()).contains("/demo?participantId="+a);
            browser.post("/demo",Map.of("participantId",""+a));
            assertThat(browser.get("/me").body()).contains("href=\"/me/benefits\"", "action=\"/demo/exit\"");
            assertThat(browser.get("/events/"+event).body()).contains("Meet &amp; Greet · Pabellón 23", "Ver resultados del evento", "Cerrar es definitivo");
        }
    }
    @Test void expiredFormUsesPingThemeAndNeverMutatesTheOriginalAction() throws Exception {
        try(var browser=new TestBrowser(port)) {
            var expired=browser.post("/events/"+event+"/close",Map.of());
            assertThat(expired.statusCode()).isEqualTo(403);
            assertThat(expired.body()).contains("Vuelve a abrir el formulario", "No se guardaron cambios", "/css/brand.css", "/js/theme-init.js", "Entrar con el código del evento");
            assertThat(expired.body()).doesNotContain("Exception", "Hibernate", "stacktrace");
            assertThat(events.get(event).getStatus()).isEqualTo(EventStatus.OPEN);
        }
    }
    @Test void unknownPageWrongMethodAndInvalidInputKeepHttpStatusAndFriendlyTheme() throws Exception {
        try(var browser=new TestBrowser(port)) {
            var unknown=browser.get("/pagina-que-no-existe");
            var wrongMethod=browser.get("/events/"+event+"/close");
            var invalid=browser.get("/admin?eventId=incorrecto");
            assertThat(unknown.statusCode()).isEqualTo(404); assertThat(wrongMethod.statusCode()).isEqualTo(405); assertThat(invalid.statusCode()).isEqualTo(400);
            for(var response:List.of(unknown,wrongMethod,invalid))
                assertThat(response.body()).contains("/css/brand.css", "Volver al inicio").doesNotContain("Exception", "Hibernate", "stacktrace");
            assertThat(events.get(event).getStatus()).isEqualTo(EventStatus.OPEN);
        }
    }
    @Test void repeatingAvailabilityFormCannotOverwriteMoreRecentState() throws Exception {
        try(var browser=new TestBrowser(port)) {
            browser.get("/demo"); browser.post("/demo",Map.of("participantId",""+a)); browser.get("/me");
            String oldVersion=""+participants.get(a).version();
            assertThat(browser.post("/me/availability",Map.of("available","true","version",oldVersion)).statusCode()).isEqualTo(302);
            var repeat=browser.post("/me/availability",Map.of("available","false","version",oldVersion));
            assertThat(repeat.statusCode()).isEqualTo(409); assertThat(participants.get(a).available()).isTrue();
            assertThat(repeat.body()).contains("Recarga la página", "/css/brand.css").doesNotContain("Exception");
        }
    }

}
