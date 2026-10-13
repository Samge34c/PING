package com.ping.participant;

import java.util.Map;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import com.ping.event.*;
import com.ping.support.TestBrowser;
import com.ping.web.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:participant-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class ParticipantTests {
    @Autowired ParticipantService service;
    @Autowired ParticipantRepository repository;
    @Autowired EventService events;
    @Autowired EventRepository eventRepository;
    @LocalServerPort int port;
    @BeforeEach void clear() { repository.deleteAll(); eventRepository.deleteAll(); }
    private Event event(boolean open) {
        EventForm form = new EventForm(); form.setName("Feria Universitaria 2026");
        Event event = events.create(form);
        if (open) events.transition(event.getId(), EventStatus.OPEN);
        return event;
    }
    private ParticipantForm form(Event event, String alias) {
        ParticipantForm form = new ParticipantForm(); form.setAlias(alias); form.setEventId(event.getId()); return form;
    }
    private AvailabilityForm availability(boolean available, long version) {
        AvailabilityForm form = new AvailabilityForm(); form.setAvailable(available); form.setVersion(version); return form;
    }
    @Test void persistsAliasAndEventWithDefaultUnavailable() {
        Event event = event(true);
        var participant = service.create(form(event, "  Samuel  "));
        var stored = service.get(participant.id());
        assertThat(stored.alias()).isEqualTo("Samuel");
        assertThat(stored.eventId()).isEqualTo(event.getId());
        assertThat(stored.eventName()).isEqualTo("Feria Universitaria 2026");
        assertThat(stored.available()).isFalse();
        assertThat(repository.findById(stored.id()).orElseThrow().getCreatedAt()).isNotNull();
    }
    @Test void availabilityChangesPersistWithoutChangingEvent() {
        var participant = service.create(form(event(true), "Julián"));
        var changed = service.setAvailability(participant.id(), availability(true, participant.version()));
        assertThat(service.get(changed.id()).available()).isTrue();
        assertThat(changed.eventId()).isEqualTo(participant.eventId());
        service.setAvailability(changed.id(), availability(false, changed.version()));
        assertThat(service.get(changed.id()).available()).isFalse();
    }
    @Test void staleAvailabilityIsRejected() {
        var participant = service.create(form(event(true), "Samuel"));
        service.setAvailability(participant.id(), availability(true, participant.version()));
        assertThatThrownBy(() -> service.setAvailability(participant.id(), availability(false, participant.version()))).isInstanceOf(BusinessRuleException.class);
    }
    @Test void validatesAliasAndMissingEvent() {
        Event event = event(true);
        assertThatThrownBy(() -> service.create(form(event, " "))).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.create(form(event, "x".repeat(81)))).isInstanceOf(ConstraintViolationException.class);
        ParticipantForm invalid = form(event, "Samuel"); invalid.setEventId(null);
        assertThatThrownBy(() -> service.create(invalid)).isInstanceOf(ConstraintViolationException.class);
        assertThat(repository.count()).isZero();
    }
    @Test void demoRegistrationRequiresOpenEventAndHandlesNonexistent() {
        assertThatThrownBy(() -> service.create(form(event(false), "Samuel"))).isInstanceOf(BusinessRuleException.class);
        ParticipantForm form = new ParticipantForm(); form.setAlias("Samuel"); form.setEventId(999999L);
        assertThatThrownBy(() -> service.create(form)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.get(999999)).isInstanceOf(NotFoundException.class);
    }
    @Test void webRegistrationAndAvailabilityWork() throws Exception {
        Event event = event(true);
        try (var browser = new TestBrowser(port)) {
            assertThat(browser.get("/").body()).contains("Participantes");
            var formPage = browser.get("/participants/new");
            assertThat(formPage.statusCode()).isEqualTo(200);
            var checkbox = java.util.regex.Pattern.compile("id=\"(available\\d+)\"").matcher(formPage.body());
            assertThat(checkbox.find()).isTrue();
            assertThat(formPage.body()).contains("for=\"" + checkbox.group(1) + "\"");
            var invalid = browser.post("/participants", Map.of("alias", " ", "eventId", event.getId().toString()));
            assertThat(invalid.statusCode()).isEqualTo(200);
            assertThat(invalid.body()).contains("Escribe un alias");
            var created = browser.post("/participants", Map.of("alias", "Samuel", "eventId", event.getId().toString()));
            assertThat(created.statusCode()).isEqualTo(302);
            String path = created.headers().firstValue("Location").orElseThrow();
            assertThat(browser.get(path).body()).contains("Samuel", "Feria Universitaria 2026", "No disponible");
            long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
            assertThat(browser.post(path + "/availability", Map.of("available", "true", "version", String.valueOf(service.get(id).version()))).statusCode()).isEqualTo(302);
            assertThat(browser.get(path).body()).contains("Marcar no disponible");
            assertThat(browser.get("/participants").body()).contains("Samuel", "Disponible");
            assertThat(browser.get("/participants/999999").statusCode()).isEqualTo(404);
        }
    }
    @Test void mutationsRejectMissingTokenAndInvalidAvailability() throws Exception {
        var participant = service.create(form(event(true), "Samuel"));
        try (var browser = new TestBrowser(port)) {
            assertThat(browser.post("/participants/" + participant.id() + "/availability", Map.of("available", "true", "version", "0")).statusCode()).isEqualTo(403);
            browser.get("/participants/" + participant.id());
            var invalid = browser.post("/participants/" + participant.id() + "/availability", Map.of("available", "invalid", "version", "0"));
            assertThat(invalid.statusCode()).isEqualTo(200);
            assertThat(invalid.body()).contains("disponibilidad válida");
            assertThat(service.get(participant.id()).available()).isFalse();
        }
    }
}
