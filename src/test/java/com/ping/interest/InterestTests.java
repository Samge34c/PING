package com.ping.interest;

import java.util.List;
import java.util.Map;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
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
        properties = "spring.datasource.url=jdbc:h2:mem:interest-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class InterestTests {
    @Autowired InterestService service;
    @Autowired ParticipantService participants;
    @Autowired ParticipantRepository participantRepository;
    @Autowired EventService events;
    @Autowired EventRepository eventRepository;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;
    @BeforeEach void clear() { participantRepository.deleteAll(); eventRepository.deleteAll(); }
    private ParticipantView participant() {
        EventForm eventForm = new EventForm(); eventForm.setName("Feria Universitaria 2026");
        Event event = events.create(eventForm); events.transition(event.getId(), EventStatus.OPEN);
        ParticipantForm form = new ParticipantForm(); form.setAlias("Samuel"); form.setEventId(event.getId()); form.setAvailable(true);
        return participants.create(form);
    }
    private long id(String name) { return service.catalog().stream().filter(interest -> interest.getName().equals(name)).findFirst().orElseThrow().getId(); }
    private InterestSelectionForm selection(long participantId, List<Long> ids) {
        InterestSelectionForm form = service.selection(participantId); form.setInterestIds(ids); return form;
    }
    @Test void demoCatalogIsSeededAndExplicitlyMarked() {
        assertThat(service.catalog().stream().filter(Interest::isDemo)).hasSize(8);
        assertThat(service.catalog()).hasSize(264);
        assertThat(service.catalog().stream().filter(Interest::isDemo).map(Interest::getName)).containsExactlyInAnyOrder("Videojuegos", "Música", "Cine", "Deportes", "Tecnología", "Anime", "Viajes", "Fotografía");
    }
    @Test void selectionPersistsAsManyToManyAndIsVisibleInParticipant() {
        var participant = participant();
        service.save(participant.id(), selection(participant.id(), List.of(id("Videojuegos"), id("Tecnología"), id("Música"))));
        assertThat(participants.get(participant.id()).interests()).containsExactlyInAnyOrder("Videojuegos", "Tecnología", "Música");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participant_interests WHERE participant_id=?", Integer.class, participant.id())).isEqualTo(3);
        assertThat(participants.get(participant.id()).eventId()).isEqualTo(participant.eventId());
        assertThat(participants.get(participant.id()).available()).isTrue();
    }
    @Test void duplicateIdsDoNotDuplicateLinksAndSelectionCanBeCleared() {
        var participant = participant();
        service.save(participant.id(), selection(participant.id(), List.of(id("Música"), id("Música"))));
        assertThat(participants.get(participant.id()).interests()).containsExactly("Música");
        service.save(participant.id(), selection(participant.id(), List.of()));
        assertThat(participants.get(participant.id()).interests()).isEmpty();
    }
    @Test void unknownInterestDoesNotOverwritePreviousSelection() {
        var participant = participant();
        service.save(participant.id(), selection(participant.id(), List.of(id("Música"))));
        assertThatThrownBy(() -> service.save(participant.id(), selection(participant.id(), List.of(id("Videojuegos"), 999999L)))).isInstanceOf(BusinessRuleException.class);
        assertThat(participants.get(participant.id()).interests()).containsExactly("Música");
    }
    @Test void invalidIdsAndNonexistentParticipantFail() {
        var participant = participant();
        assertThatThrownBy(() -> service.save(participant.id(), selection(participant.id(), List.of(-1L)))).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.selection(999999)).isInstanceOf(NotFoundException.class);
    }
    @Test void staleSelectionIsRejected() {
        var participant = participant();
        InterestSelectionForm original = selection(participant.id(), List.of(id("Música")));
        service.save(participant.id(), original);
        original.setInterestIds(List.of(id("Cine")));
        assertThatThrownBy(() -> service.save(participant.id(), original)).isInstanceOf(BusinessRuleException.class);
    }
    @Test void webCatalogAndMultipleSelectionWork() throws Exception {
        var participant = participant();
        try (var browser = new TestBrowser(port)) {
            assertThat(browser.get("/interests").body()).contains("Datos de demostración", "Videojuegos");
            String path = "/participants/" + participant.id() + "/interests";
            var form = browser.get(path);
            assertThat(form.statusCode()).isEqualTo(200);
            var checkbox = java.util.regex.Pattern.compile("id=\"(interestIds\\d+)\"").matcher(form.body());
            assertThat(checkbox.find()).isTrue();
            assertThat(form.body()).contains("for=\"" + checkbox.group(1) + "\"");
            var result = browser.postMulti(path, Map.of("interestIds", List.of(String.valueOf(id("Videojuegos")), String.valueOf(id("Música"))), "version", List.of(String.valueOf(participants.get(participant.id()).version()))));
            assertThat(result.statusCode()).isEqualTo(302);
            assertThat(browser.get("/participants/" + participant.id()).body()).contains("Videojuegos", "Música", "Samuel");
            assertThat(browser.get(path).body()).contains("checked=\"checked\"");
            assertThat(browser.get("/participants/999999/interests").statusCode()).isEqualTo(404);
        }
    }
    @Test void badFormAndMissingTokenDoNotWriteLinks() throws Exception {
        var participant = participant();
        String path = "/participants/" + participant.id() + "/interests";
        try (var browser = new TestBrowser(port)) {
            assertThat(browser.post(path, Map.of("interestIds", "1", "version", "0")).statusCode()).isEqualTo(403);
            browser.get(path);
            var result = browser.post(path, Map.of("interestIds", "abc", "version", "0"));
            assertThat(result.statusCode()).isEqualTo(200);
            assertThat(result.body()).contains("Revisa la selección");
            assertThat(participants.get(participant.id()).interests()).isEmpty();
        }
    }
}
