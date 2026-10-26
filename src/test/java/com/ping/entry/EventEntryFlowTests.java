package com.ping.entry;

import com.ping.event.*;
import com.ping.participant.*;
import com.ping.interest.*;
import com.ping.match.*;
import com.ping.support.TestBrowser;
import java.util.*;
import java.util.regex.Pattern;
import java.io.*;
import javax.imageio.ImageIO;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "spring.datasource.url=jdbc:h2:mem:event-entry-flow;MODE=MySQL;DB_CLOSE_DELAY=-1", "ping.public-base-url=https://ping.example.test"})
@ActiveProfiles("test")
class EventEntryFlowTests {
    @Autowired EventService events;
    @Autowired EventRepository eventRepository;
    @Autowired EventEntryLinks links;
    @Autowired ParticipantRepository people;
    @Autowired ParticipantService participants;
    @Autowired InterestService interests;
    @Autowired MatchService matches;
    @Autowired MatchingService matching;
    @LocalServerPort int port;
    Event event;
    @BeforeEach void setup() {event=create("SOFA 2026");}
    Event create(String name) {var f=new EventForm();f.setName(name);var e=events.create(f);events.transition(e.getId(),EventStatus.OPEN);return events.get(e.getId());}
    Map<String,String> fields(String html) {
        var data=new HashMap<String,String>();
        for(String name:List.of("draftKey","revision")) {var m=Pattern.compile("name=\""+name+"\"[^>]*value=\"([^\"]*)\"").matcher(html);assertThat(m.find()).isTrue();data.put(name,m.group(1));}return data;
    }
    void identify(TestBrowser b) throws Exception {assertThat(b.get("/join?code="+event.getEntryCode()).statusCode()).isEqualTo(302);}
    Map<String,String> alias(TestBrowser b,String alias) throws Exception {var f=fields(b.get("/join").body());f.put("alias",alias);f.put("eventId",""+event.getId());assertThat(b.post("/join",f).statusCode()).isEqualTo(302);return fields(b.get("/join/interests").body());}
    long enter(TestBrowser b,String name) throws Exception {
        identify(b);var f=alias(b,name);f.put("interestIds",""+interests.catalog().getFirst().getId());b.post("/join/interests",f);
        return people.findAll().stream().filter(p->p.getAlias().equals(name)).findFirst().orElseThrow().getId();
    }
    @Test void codeIsUniqueStableNormalizedAndRespectsAllEventStates() throws Exception {
        Event other=create("Otro");assertThat(other.getEntryCode()).isNotEqualTo(event.getEntryCode());
        assertThat(events.entryEvent(" "+event.getEntryCode().toLowerCase(Locale.ROOT)+" ").getId()).isEqualTo(event.getId());
        String code=event.getEntryCode();var form=EventForm.from(event);form.setName("Nuevo nombre");events.update(event.getId(),form);assertThat(events.get(event.getId()).getEntryCode()).isEqualTo(code);
        try(var b=new TestBrowser(port)) {
            var f=fields(b.get("/join").body());f.put("code","???");assertThat(b.post("/join/event",f).body()).contains("Revisa el código");
            f.put("code","ZZZZZZZZZZZZ");assertThat(b.post("/join/event",f).statusCode()).isEqualTo(404);
            f.put("code",code.toLowerCase(Locale.ROOT));assertThat(b.post("/join/event",f).statusCode()).isEqualTo(302);
            assertThat(b.get("/join").body()).contains("Nuevo nombre").doesNotContain("<select");
            events.transition(event.getId(),EventStatus.PAUSED);assertThat(b.get("/join?code="+code).body()).contains("en pausa");
            events.transition(event.getId(),EventStatus.CLOSED);assertThat(b.get("/join?code="+code).body()).contains("cerrado");
            var draft=new EventForm();draft.setName("Aún no");var notOpen=events.create(draft);
            assertThat(b.get("/join?code="+notOpen.getEntryCode()).body()).contains("todavía no ha abierto");
        }
    }
    @Test void qrIsLocalPngContainsOnlyConfiguredEventLinkAndTheLinkIdentifiesThatEvent() throws Exception {
        String expected="https://ping.example.test/join?code="+event.getEntryCode();assertThat(links.link(event)).isEqualTo(expected);
        var image=ImageIO.read(new ByteArrayInputStream(links.qr(event)));int width=image.getWidth(),height=image.getHeight();
        var source=new RGBLuminanceSource(width,height,image.getRGB(0,0,width,height,null,0,width));
        String decoded=new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source))).getText();assertThat(decoded).isEqualTo(expected);
        assertThat(decoded).doesNotContain("jsessionid","participantId","PIXEL");
        try(var b=new TestBrowser(port)) {
            assertThat(b.get("/events/"+event.getId()+"/qr.png?download=true").headers().firstValue("Content-Disposition").orElseThrow()).startsWith("attachment;").contains(event.getEntryCode());
            var uri=java.net.URI.create(decoded);assertThat(b.get(uri.getRawPath()+"?"+uri.getRawQuery()).statusCode()).isEqualTo(302);
            assertThat(b.get("/join").body()).contains(event.getName());
        }
        assertThat(new EventEntryLinks("").link(event)).isNull();
        assertThatThrownBy(()->new EventEntryLinks("").qr(event)).isInstanceOf(com.ping.web.BusinessRuleException.class);
        for(String invalid:List.of("/relative","ftp://example.test","https://user:pass@example.test","https://example.test/?session=x","https://example.test/#x")) assertThatThrownBy(()->new EventEntryLinks(invalid)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void eventChangePreservesAliasInterestsAndRejectsStaleOrTamperedSelection() throws Exception {
        try(var b=new TestBrowser(port)) {
            identify(b);var f=alias(b,"Alias conservado");String interest=""+interests.catalog().getFirst().getId();f.put("interestIds",interest);f.put("action","back");b.post("/join/interests",f);
            f=fields(b.get("/join").body());f.put("alias","Alias corregido");f.put("eventId",""+event.getId());f.put("action","changeEvent");b.post("/join",f);
            var codeFields=fields(b.get("/join/event").body());Event next=create("Segunda convención");codeFields.put("code",next.getEntryCode());b.post("/join/event",codeFields);
            assertThat(b.get("/join").body()).contains("Alias corregido","Segunda convención");assertThat(b.post("/join/event",codeFields).statusCode()).isEqualTo(409);
            f=fields(b.get("/join").body());f.put("alias","Alias corregido");f.put("eventId",""+event.getId());assertThat(b.post("/join",f).body()).contains("Usa Cambiar evento");
            f.put("eventId",""+next.getId());b.post("/join",f);assertThat(b.get("/join/interests").body()).contains("checked=\"checked\"","Segunda convención");
            var finish=fields(b.get("/join/interests").body());finish.put("interestIds",interest);long before=people.count();b.post("/join/interests",finish);b.post("/join/interests",finish);assertThat(people.count()).isEqualTo(before+1);
            var p=people.findAll().stream().filter(v->v.getAlias().equals("Alias corregido")).findFirst().orElseThrow();assertThat(participants.get(p.getId()).eventId()).isEqualTo(next.getId());assertThat(p.isAvailable()).isFalse();
        }
    }
    @Test void unavailableEventDoesNotOfferActivationOrSearchAndKeepsProfileAccessible() throws Exception {
        try(var b=new TestBrowser(port)) {
            long id=enter(b,"Pausa "+event.getId());
            for(var state:List.of(EventStatus.PAUSED,EventStatus.CLOSED)) {
                events.transition(event.getId(),state);
                assertThat(b.get("/connect").body()).doesNotContain("action=\"/me/availability\"","action=\"/me/search\"");
                assertThat(b.get("/me").body()).contains("El evento no permite nuevas conexiones ahora");
                assertThat(participants.get(id).available()).isFalse();assertThat(matches.list(id)).isEmpty();
            }
        }
    }
    @Test void connectIsReadOnlyResolvesProposalAndActiveAndKeepsPersonalProfileSeparate() throws Exception {
        try(var guest=new TestBrowser(port);var a=new TestBrowser(port);var b=new TestBrowser(port)) {
            assertThat(guest.get("/connect").headers().firstValue("Location").orElseThrow()).endsWith("/join");
            long aid=enter(a,"A "+event.getId()),bid=enter(b,"B "+event.getId());
            assertThat(a.get("/connect").body()).contains("Activar disponibilidad");assertThat(participants.get(aid).available()).isFalse();assertThat(matches.list(aid)).isEmpty();
            assertThat(a.get("/me").body()).contains("Tus gustos","Ir a Conectar").doesNotContain("data-search-form");
            for(var pair:List.of(Map.entry(a,aid),Map.entry(b,bid))) {
                pair.getKey().get("/connect");assertThat(pair.getKey().post("/me/availability",Map.of("version",""+participants.get(pair.getValue()).version(),"available","true","returnTo","connect")).headers().firstValue("Location").orElseThrow()).endsWith("/connect");
            }
            assertThat(a.get("/connect").body()).contains("Buscar conexión");assertThat(matches.list(aid)).isEmpty();
            String location=a.post("/me/search",Map.of("returnTo","connect")).headers().firstValue("Location").orElseThrow();long id=Long.parseLong(location.substring(location.lastIndexOf('/')+1));
            for(var browser:List.of(a,b)) assertThat(browser.get("/connect").headers().firstValue("Location").orElseThrow()).endsWith("/matches/"+id);
            a.get(location);a.post(location+"/accept",Map.of());b.get(location);b.post(location+"/accept",Map.of());
            assertThat(matches.get(id,aid).status()).isEqualTo(MatchStatus.ACTIVE);assertThat(a.get("/connect").headers().firstValue("Location").orElseThrow()).endsWith("/matches/"+id);
            assertThat(a.get("/me").body()).doesNotContain("PIXEL-","data-other-hint");assertThat(a.get("/demo").statusCode()).isEqualTo(200);assertThat(a.get("/events").statusCode()).isEqualTo(200);
            a.post(location+"/arrive",Map.of());b.post(location+"/arrive",Map.of());
            a.get(location);a.post(location+"/confirm",Map.of());assertThat(participants.get(aid).points()).isZero();b.get(location);b.post(location+"/confirm",Map.of());assertThat(participants.get(aid).points()).isEqualTo(10);assertThat(participants.get(bid).points()).isEqualTo(10);
            assertThat(a.get("/connect").statusCode()).isEqualTo(200);
        }
    }
}
