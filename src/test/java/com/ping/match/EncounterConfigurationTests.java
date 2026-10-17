package com.ping.match;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import com.ping.event.*;
import com.ping.participant.*;
import com.ping.support.TestBrowser;
import com.ping.web.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:encounter-config;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class EncounterConfigurationTests {
    @Autowired EventService events;
    @Autowired ParticipantService people;
    @Autowired MatchService matches;
    @Autowired MatchingService matching;
    @Autowired JdbcTemplate sql;
    @LocalServerPort int port;
    long event, a, b, id;
    @BeforeEach void prepare() {
        sql.update("delete from feedback"); sql.update("delete from match_reservations"); sql.update("delete from ping_matches");
        sql.update("delete from participants"); sql.update("delete from events");
        EventForm f = new EventForm(); f.setName("Encuentro de prueba"); f.setMeetingPoint("Entrada principal");
        f.setMeetingPoints(" Patio público \nMesa de bienvenida\nPatio público\n");
        f.setEncounterWords("LUNA\nSOL"); f.setEncounterActivities("Compartan una recomendación.\nElijan una pregunta para conversar.");
        event = events.create(f).getId(); events.transition(event, EventStatus.OPEN);
        a = participant(event,"Lina"); b = participant(event,"Nico"); id = matching.search(a);
    }
    long participant(long eventId, String alias) {
        ParticipantForm p = new ParticipantForm(); p.setAlias(alias); p.setEventId(eventId); p.setAvailable(true); return people.create(p).id();
    }
    void activate() { matches.accept(id,a); matches.accept(id,b); }
    @Test void catalogsAreNormalizedScopedAndFrozenOnlyAtBilateralAcceptance() {
        assertThat(events.get(event).getMeetingPoints()).isEqualTo("Patio público\nMesa de bienvenida");
        matches.accept(id,a); assertThat(matches.get(id,a).meetingPoint()).isNull(); assertThat(matches.get(id,a).word()).isNull();
        matches.accept(id,b); var before = matches.get(id,a);
        assertThat(before.meetingPoint()).isIn("Patio público","Mesa de bienvenida");
        assertThat(before.word()).endsWith("-"+Long.toString(id,36).toUpperCase(Locale.ROOT));
        assertThat(before.activity()).isIn("Compartan una recomendación.","Elijan una pregunta para conversar.");
        EventForm edit = EventForm.from(events.get(event)); edit.setMeetingPoints("Nueva plaza"); edit.setEncounterWords("OTRA"); edit.setEncounterActivities("Otra actividad."); events.update(event,edit);
        matches.accept(id,a); assertThat(matches.get(id,b).meetingPoint()).isEqualTo(before.meetingPoint());
        assertThat(matches.get(id,a).word()).isEqualTo(before.word()); assertThat(matches.get(id,a).activity()).isEqualTo(before.activity());
        EventForm other = new EventForm(); other.setName("Otro evento"); other.setMeetingPoint("Entrada de otro evento"); long e = events.create(other).getId(); events.transition(e,EventStatus.OPEN);
        long c=participant(e,"Otra A"),d=participant(e,"Otra B"),m=matching.search(c); matches.accept(m,c);matches.accept(m,d);
        assertThat(matches.get(m,c).meetingPoint()).isEqualTo("Entrada de otro evento"); assertThat(matches.get(m,c).word()).startsWith("PING-");
    }
    @Test void optionalListsIndependentlyPreserveExistingCatalogAndRejectOversizedEntries() {
        EventForm edit=EventForm.from(events.get(event)); edit.setEncounterWords(""); edit.setEncounterActivities(""); events.update(event,edit); activate();
        assertThat(matches.get(id,a).word()).startsWith("PING-"); assertThat(matches.get(id,a).activity()).isEqualTo(ActivityCatalog.activity(""));
        edit=EventForm.from(events.get(event)); edit.setMeetingPoints("x".repeat(161)); EventForm invalid=edit;
        assertThatThrownBy(()->events.update(event,invalid)).isInstanceOf(jakarta.validation.ConstraintViolationException.class);
        assertThat(events.get(event).getMeetingPoints()).isEqualTo("Patio público\nMesa de bienvenida");
        assertThatThrownBy(()->EncounterOptions.normalize("x\n".repeat(21),160,"Puntos")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(()->EncounterOptions.normalize("hola\u0000",40,"Palabras")).isInstanceOf(BusinessRuleException.class);
    }
    @ParameterizedTest @EnumSource(value=EventStatus.class,names={"PAUSED","CLOSED"})
    void arrivalIsBilateralAndNeverRewardsAndActiveCanFinishWhenEventStops(EventStatus status) {
        assertThatThrownBy(()->matches.arrive(id,a)).isInstanceOf(BusinessRuleException.class);
        activate(); events.transition(event,status);
        assertThatThrownBy(()->matches.confirm(id,a)).isInstanceOf(BusinessRuleException.class);
        matches.arrive(id,a); matches.arrive(id,a);
        assertThat(matches.get(id,a).arrived()).isTrue(); assertThat(matches.get(id,b).otherArrived()).isTrue();
        assertThat(matches.get(id,b).arrived()).isFalse(); assertThat(matches.get(id,a).confirmed()).isFalse();
        assertThatThrownBy(()->matches.confirm(id,b)).isInstanceOf(BusinessRuleException.class);
        assertThat(people.get(a).points()).isZero(); matches.arrive(id,b); assertThat(people.get(b).points()).isZero();
        matches.confirm(id,a); assertThat(people.get(a).points()).isZero(); matches.confirm(id,b); matches.confirm(id,b);
        assertThat(matches.get(id,a).status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(people.get(a).points()).isEqualTo(10); assertThat(people.get(b).points()).isEqualTo(10);
    }
    @Test void concurrentArrivalRetriesKeepEachPersonsStateAndInstructions() throws Exception {
        activate(); var before=matches.get(id,a);
        try(var pool=Executors.newFixedThreadPool(4)) {
            CountDownLatch start=new CountDownLatch(1); List<Future<?>> requests=new ArrayList<>();
            for(int i=0;i<4;i++) {long person=i%2==0?a:b; requests.add(pool.submit(()->{start.await(); matches.arrive(id,person);return null;}));}
            start.countDown(); for(var request:requests)request.get(10,TimeUnit.SECONDS);
        }
        assertThat(matches.get(id,a).canConfirm()).isTrue(); assertThat(matches.get(id,a).word()).isEqualTo(before.word());
        assertThat(people.get(a).points()).isZero(); assertThat(matches.get(id,a).confirmed()).isFalse();
    }
    @Test void oldActiveWithPartialConfirmationKeepsItsConditions() {
        activate(); sql.update("update ping_matches set arrival_required=false, first_confirmed=true where id=?",id);
        matches.confirm(id,b); assertThat(matches.get(id,a).status()).isEqualTo(MatchStatus.COMPLETED); assertThat(people.get(a).points()).isEqualTo(10);
    }
    @Test void arrivalRequiresOwnSessionAndCsrfAndPollingContainsNoPrivateInstructions() throws Exception {
        long outsider=participant(event,"Ajeno"); activate();
        assertThatThrownBy(()->matches.arrive(id,outsider)).isInstanceOf(NotFoundException.class);
        try(var self=new TestBrowser(port);var other=new TestBrowser(port);var anonymous=new TestBrowser(port)) {
            self.get("/demo");self.post("/demo",Map.of("participantId",""+a));other.get("/demo");other.post("/demo",Map.of("participantId",""+outsider));
            String path="/matches/"+id; var page=self.get(path);
            assertThat(page.body()).contains("Ya llegué").doesNotContain("action=\""+path+"/confirm\"");
            assertThat(anonymous.post(path+"/arrive",Map.of()).statusCode()).isEqualTo(403);
            assertThat(other.post(path+"/arrive",Map.of("participantId",""+a)).statusCode()).isEqualTo(404);
            assertThat(self.get(path+"/arrive").statusCode()).isEqualTo(405);
            String initial=self.get("/me/state").body();
            assertThat(self.post(path+"/arrive",Map.of("participantId",""+b)).statusCode()).isEqualTo(302);
            assertThat(matches.get(id,b).arrived()).isFalse();
            assertThat(self.get("/me/state").body()).isNotEqualTo(initial).doesNotContain(matches.get(id,a).word(),matches.get(id,a).meetingPoint(),matches.get(id,a).activity());
            self.post(path+"/arrive",Map.of()); assertThat(people.get(a).points()).isZero();
            matches.arrive(id,b); assertThat(self.get(path).body()).contains("Ambos llegaron","Confirmar encuentro y actividad");
            matches.cancel(id,a); assertThatThrownBy(()->matches.arrive(id,a)).isInstanceOf(BusinessRuleException.class);
        }
    }
    @Test void invalidCatalogFormKeepsDraftAndDoesNotWriteAndVersionStillProtectsEdits() throws Exception {
        try(var browser=new TestBrowser(port)) {
            browser.get("/events/"+event+"/edit");
            var response=browser.post("/events/"+event+"/edit",Map.of("name","Conservar borrador","version",""+events.get(event).getVersion(),"encounterWords","x".repeat(41)));
            assertThat(response.statusCode()).isEqualTo(200); assertThat(response.body()).contains("Conservar borrador","Usa hasta 20 opciones");
            assertThat(events.get(event).getName()).isEqualTo("Encuentro de prueba");
        }
        EventForm stale=EventForm.from(events.get(event)); EventForm valid=EventForm.from(events.get(event));valid.setEncounterWords("NUEVA");events.update(event,valid);
        assertThatThrownBy(()->events.update(event,stale)).isInstanceOf(BusinessRuleException.class);
    }
}
