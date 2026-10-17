package com.ping.entry;

import com.ping.event.*;
import com.ping.participant.*;
import com.ping.interest.*;
import com.ping.support.TestBrowser;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties="spring.datasource.url=jdbc:h2:mem:entry-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class ParticipantEntryTests {
    @Autowired ParticipantRepository people;
    @Autowired ParticipantService participants;
    @Autowired EventRepository eventRepository;
    @Autowired EventService events;
    @Autowired InterestService interests;
    @Autowired ParticipantEntryService entry;
    @LocalServerPort int port;
    long event;
    @BeforeEach void setup() {
        people.deleteAll(); eventRepository.deleteAll();
        EventForm f=new EventForm(); f.setName("Encuentro aislado"); event=events.create(f).getId(); events.transition(event,EventStatus.OPEN);
    }
    Map<String,String> fields(String page) {
        Map<String,String> data=new HashMap<>();
        for(String name: List.of("draftKey","revision")) {
            var m=Pattern.compile("name=\""+name+"\"[^>]*value=\"([^\"]*)\"").matcher(page); assertThat(m.find()).isTrue(); data.put(name,m.group(1));
        }
        return data;
    }
    String open(TestBrowser b) throws Exception {
        b.get("/join?code="+events.get(event).getEntryCode()); return b.get("/join").body();
    }
    void details(TestBrowser b,String alias) throws Exception {
        var f=fields(open(b)); f.put("alias",alias); f.put("eventId",""+event);
        assertThat(b.post("/join",f).headers().firstValue("Location").orElseThrow()).endsWith("/join/interests");
    }
    @Test void ownEntryCorrectsDraftAndUsesExistingAvailabilityAndSearch() throws Exception {
        try(var b=new TestBrowser(port)) {
            var data=fields(open(b));data.put("alias"," ");data.put("eventId",""+event);
            assertThat(b.post("/join",data).body()).contains("Escribe un alias");assertThat(people.count()).isZero();
            details(b,"Alias inicial");var f=fields(b.get("/join/interests").body());String interest=""+interests.catalog().getFirst().getId();f.put("interestIds",interest);f.put("action","back");b.post("/join/interests",f);
            var back=b.get("/join");assertThat(back.body()).contains("Alias inicial");f=fields(back.body());f.put("alias","Alias corregido");f.put("eventId",""+event);b.post("/join",f);
            String choices=b.get("/join/interests").body();assertThat(choices).contains("checked=\"checked\"");f=fields(choices);f.put("interestIds",interest);f.put("participantId","999999");
            assertThat(b.post("/join/interests",f).headers().firstValue("Location").orElseThrow()).endsWith("/me");
            var person=people.findAll().getFirst();assertThat(person.getAlias()).isEqualTo("Alias corregido");assertThat(person.isAvailable()).isFalse();assertThat(participants.get(person.getId()).interests()).hasSize(1);assertThat(person.getPoints()).isZero();
            assertThat(b.get("/me").body()).contains("Alias corregido","Ir a Conectar").doesNotContain("/demo/prepare", "Cambiar participante demo");
            b.post("/me/availability",Map.of("available","true","version",""+person.getVersion()));
            b.post("/me/search",Map.of());assertThat(b.get("/me").body()).contains("No hay personas disponibles");
            assertThat(b.post("/join/interests",f).headers().firstValue("Location").orElseThrow()).endsWith("/me");assertThat(people.count()).isEqualTo(1);
        }
    }
    @Test void staleDetailsCannotOverwriteSavedDraft() throws Exception {
        try(var b=new TestBrowser(port)) {
            var f=fields(open(b)); f.put("alias","Alias guardado"); f.put("eventId",""+event); b.post("/join",f);
            f.put("alias","Cambio obsoleto"); var rejected=b.post("/join",f);
            assertThat(rejected.body()).contains("otra pestaña", "Alias guardado").doesNotContain("value=\"Cambio obsoleto\"");
            assertThat(people.count()).isZero();
        }
    }
    @Test void concurrentFinishAndReplayCreateOnlyOneParticipant() throws Exception {
        try(var b=new TestBrowser(port);var pool=Executors.newFixedThreadPool(2)) {
            details(b,"Una sola entrada");var f=fields(b.get("/join/interests").body());CountDownLatch start=new CountDownLatch(1);
            Callable<Integer> call=()->{start.await();return b.post("/join/interests",f).statusCode();};var first=pool.submit(call);var second=pool.submit(call);start.countDown();
            assertThat(first.get()).isEqualTo(302);assertThat(second.get()).isEqualTo(302);assertThat(people.count()).isEqualTo(1);assertThat(b.get("/me").body()).contains("Una sola entrada");
        }
    }
    @Test void csrfAndForeignDraftCannotAdoptParticipant() throws Exception {
        try(var a=new TestBrowser(port);var b=new TestBrowser(port)) {
            assertThat(a.post("/join",Map.of()).statusCode()).isEqualTo(403);details(a,"Propietario");var f=fields(a.get("/join/interests").body());a.post("/join/interests",f);
            b.get("/join");f.put("participantId",""+people.findAll().getFirst().getId());assertThat(b.post("/join/interests",f).statusCode()).isEqualTo(409);
            assertThat(b.get("/me").headers().firstValue("Location").orElseThrow()).endsWith("/join");assertThat(people.count()).isEqualTo(1);
            details(b,"Otra entrada propia"); var own=fields(b.get("/join/interests").body()); own.put("participantId",""+people.findAll().getFirst().getId());
            b.post("/join/interests",own); assertThat(b.get("/me").body()).contains("Otra entrada propia").doesNotContain("Propietario"); assertThat(people.count()).isEqualTo(2);
            a.get("/me");a.post("/demo/exit",Map.of());assertThat(a.post("/join/interests",f).statusCode()).isEqualTo(409);
            assertThat(a.get("/me").headers().firstValue("Location").orElseThrow()).endsWith("/join");
        }
    }
    @Test void staleRevisionAndInvalidInterestsKeepDraftAndCreateNothing() throws Exception {
        try(var b=new TestBrowser(port)) {
            details(b,"Se conserva");var f=fields(b.get("/join/interests").body());f.put("action","back");b.post("/join/interests",f);
            f.put("action","complete");assertThat(b.post("/join/interests",f).statusCode()).isEqualTo(409);assertThat(people.count()).isZero();
            details(b,"Se conserva");f=fields(b.get("/join/interests").body());f.put("interestIds","999999");assertThat(b.post("/join/interests",f).statusCode()).isEqualTo(409);assertThat(people.count()).isZero();assertThat(b.get("/join").body()).contains("Se conserva");
        }
    }
    @Test void eventClosingIsRecoverableAndServiceRollsBackWholeEntry() throws Exception {
        assertThatThrownBy(()->entry.create("No parcial",event,List.of(999999L))).isInstanceOf(com.ping.web.BusinessRuleException.class);assertThat(people.count()).isZero();
        try(var b=new TestBrowser(port)) {
            details(b,"Espera apertura");var f=fields(b.get("/join/interests").body());events.transition(event,EventStatus.PAUSED);
            assertThat(b.post("/join/interests",f).statusCode()).isEqualTo(409);assertThat(people.count()).isZero();
            events.transition(event,EventStatus.OPEN);assertThat(b.get("/join").body()).contains("Espera apertura");
            assertThat(b.post("/join/interests",f).statusCode()).isEqualTo(302);assertThat(people.count()).isEqualTo(1);
        }
    }
}
