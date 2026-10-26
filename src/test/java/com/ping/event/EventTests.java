package com.ping.event;

import java.util.Map;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import com.ping.support.TestBrowser;
import com.ping.web.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:event-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class EventTests {
    @Autowired EventService service;
    @Autowired EventRepository repository;
    @LocalServerPort int port;
    @BeforeEach void clear() { repository.deleteAll(); }
    private Event create(String name) {
        EventForm form = new EventForm(); form.setName(name); form.setDescription("Descripción");
        return service.create(form);
    }
    private Event inState(EventStatus status) {
        Event event = create("Feria");
        if (status != EventStatus.DRAFT) service.transition(event.getId(), EventStatus.OPEN);
        if (status == EventStatus.PAUSED) service.transition(event.getId(), EventStatus.PAUSED);
        if (status == EventStatus.CLOSED) service.transition(event.getId(), EventStatus.CLOSED);
        return service.get(event.getId());
    }
    @Test void createsAndPersistsDraftWithTimestamps() {
        Event event = create("  Feria Universitaria 2026  ");
        Event stored = repository.findById(event.getId()).orElseThrow();
        assertThat(stored.getName()).isEqualTo("Feria Universitaria 2026");
        assertThat(stored.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getUpdatedAt()).isNotNull();
        assertThat(stored.canStartNewMatches()).isFalse();
    }
    @ParameterizedTest
    @CsvSource({"DRAFT,OPEN,true", "OPEN,PAUSED,true", "PAUSED,OPEN,true", "OPEN,CLOSED,true", "PAUSED,CLOSED,true",
            "DRAFT,DRAFT,false", "DRAFT,PAUSED,false", "DRAFT,CLOSED,false", "OPEN,DRAFT,false", "OPEN,OPEN,false",
            "PAUSED,DRAFT,false", "PAUSED,PAUSED,false", "CLOSED,DRAFT,false", "CLOSED,OPEN,false", "CLOSED,PAUSED,false", "CLOSED,CLOSED,false"})
    void permitsOnlyDocumentedTransitions(EventStatus source, EventStatus target, boolean allowed) {
        Event event = inState(source);
        if (allowed) {
            service.transition(event.getId(), target);
            assertThat(service.get(event.getId()).getStatus()).isEqualTo(target);
        } else {
            assertThatThrownBy(() -> service.transition(event.getId(), target)).isInstanceOf(BusinessRuleException.class);
            assertThat(service.get(event.getId()).getStatus()).isEqualTo(source);
        }
    }
    @Test void onlyOpenAllowsNewMatches() {
        for (EventStatus status : EventStatus.values()) assertThat(inState(status).canStartNewMatches()).isEqualTo(status == EventStatus.OPEN);
    }
    @Test void validatesNameAndLengthAtServiceBoundary() {
        assertThatThrownBy(() -> create("  ")).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> create("x".repeat(121))).isInstanceOf(ConstraintViolationException.class);
        assertThat(repository.count()).isZero();
    }
    @Test void updatesDetailsWithoutChangingStateAndRejectsStaleForm() {
        Event event = inState(EventStatus.OPEN);
        EventForm form = EventForm.from(event); form.setName("Feria actualizada");
        service.update(event.getId(), form);
        Event changed = service.get(event.getId());
        assertThat(changed.getName()).isEqualTo("Feria actualizada");
        assertThat(changed.getStatus()).isEqualTo(EventStatus.OPEN);
        assertThat(changed.getCreatedAt()).isEqualTo(event.getCreatedAt());
        assertThatThrownBy(() -> service.update(event.getId(), form)).isInstanceOf(BusinessRuleException.class);
    }
    @Test void nonexistentEventFailsClearly() { assertThatThrownBy(() -> service.get(999999)).isInstanceOf(NotFoundException.class); }
    @Test void webCreatesEditsAndChangesEventState() throws Exception {
        try (var browser = new TestBrowser(port)) {
            assertThat(browser.get("/").body()).contains("Conecta en tu evento por lo que te gusta.", "Unirme a un evento", "Probar la demo");
            assertThat(browser.get("/events/new").statusCode()).isEqualTo(200);
            var invalid = browser.post("/events", Map.of("name", " "));
            assertThat(invalid.statusCode()).isEqualTo(200);
            assertThat(invalid.body()).contains("Escribe el nombre");
            assertThat(repository.count()).isZero();
            var created = browser.post("/events", Map.of("name", "Feria", "description", "<script>alert(1)</script>", "status", "CLOSED"));
            assertThat(created.statusCode()).isEqualTo(302);
            String path = created.headers().firstValue("Location").orElseThrow();
            var detail = browser.get(path);
            assertThat(detail.statusCode()).isEqualTo(200);
            assertThat(detail.body()).contains("Borrador", "&lt;script&gt;").doesNotContain("<script>alert");
            long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
            assertThat(browser.get(path + "/edit").statusCode()).isEqualTo(200);
            var edited = browser.post(path + "/edit", Map.of("name", "Feria editada", "description", "Nueva", "version", String.valueOf(service.get(id).getVersion())));
            assertThat(edited.statusCode()).isEqualTo(302);
            for (String action : new String[]{"open", "pause", "resume", "close"}) {
                browser.get(path); assertThat(browser.post(path + "/" + action, Map.of()).statusCode()).isEqualTo(302);
            }
            assertThat(browser.get(path).body()).contains("Cerrado", "Feria editada");
            assertThat(browser.post(path + "/open", Map.of()).statusCode()).isEqualTo(409);
            assertThat(browser.get("/events").body()).contains("Feria editada");
            assertThat(browser.get("/events/999999").statusCode()).isEqualTo(404);
        }
    }
    @Test void mutationsRequireFormTokenAndPost() throws Exception {
        Event event = create("Feria");
        try (var browser = new TestBrowser(port)) {
            assertThat(browser.post("/events/" + event.getId() + "/open", Map.of()).statusCode()).isEqualTo(403);
            assertThat(browser.get("/events/" + event.getId() + "/open").statusCode()).isEqualTo(405);
            assertThat(service.get(event.getId()).getStatus()).isEqualTo(EventStatus.DRAFT);
        }
    }
}
