package com.ping.match;

import java.util.stream.Stream;
import com.ping.event.*;
import com.ping.interest.*;
import com.ping.participant.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sofa-demo;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class SofaDemoTests {
    @Autowired EventService events;
    @Autowired EventRepository eventRepository;
    @Autowired ParticipantService participants;
    @Autowired ParticipantRepository people;
    @Autowired InterestService interests;
    @Autowired MatchingService matching;
    @Autowired MatchService service;
    @Autowired MatchRepository matches;
    @Autowired MatchReservationRepository reservations;
    @Autowired FeedbackRepository feedback;

    @BeforeEach void clear() {
        feedback.deleteAll(); reservations.deleteAll(); matches.deleteAll(); people.deleteAll(); eventRepository.deleteAll();
    }
    static Stream<Arguments> encounters() {
        return Stream.of(
            Arguments.of("Videojuegos", "Zona Gamer · Pabellón 18", "PIXEL", "Elijan un juego o stand de la Zona Gamer que ambos probarían y cuenten qué les llamó la atención."),
            Arguments.of("Anime", "Comunidades SOFA · Pabellón 3 Nivel 2", "MANGA", "Encuentren un personaje que ambos reconozcan y recomiéndense una serie."),
            Arguments.of("Fotografía", "Cosplay Parade · Pabellón 8 Nivel 2", "FLASH", "Busquen un rincón visual del SOFA y elijan juntos qué imagen representa mejor el evento."),
            Arguments.of("Música", "SOFA Stage · Pabellón 17", "RITMO", "Recomiéndense un opening, soundtrack o canción geek y elijan una para escuchar después."),
            Arguments.of("Tecnología", "Stage Cultura Gamer · Pabellón 17", "CODIGO", "Encuentren un proyecto o idea tecnológica del SOFA que les parezca interesante y comparen qué mejorarían."),
            Arguments.of("Cine", "Meet & Greet · Pabellón 23", "ESCENA", "Elijan una película o serie que ambos convertirían en cosplay y cuenten qué personaje escogerían."),
            Arguments.of("", "Meet & Greet · Pabellón 23", "PING", "Cada uno cuente cuál ha sido su zona favorita del SOFA y elijan una para recorrer juntos.")
        );
    }
    long person(long event, String alias, String interest) {
        ParticipantForm f = new ParticipantForm(); f.setEventId(event); f.setAlias(alias);
        long id = participants.create(f).id();
        InterestSelectionForm selection = interests.selection(id);
        selection.setInterestIds(interests.catalog().stream().filter(i -> i.getName().equals(interest)).map(Interest::getId).toList());
        interests.save(id, selection);
        AvailabilityForm available = new AvailabilityForm(); available.setAvailable(true); available.setVersion(participants.get(id).version());
        participants.setAvailability(id, available); return id;
    }
    @ParameterizedTest(name = "SOFA: {0} → {1} / {2}") @MethodSource("encounters")
    void bilateralEncounterIsSharedStableUniqueAndRewardsOnce(String interest, String point, String prefix, String activity) {
        EventForm form = new EventForm(); form.setName("SOFA 2026"); form.setMeetingPoint("Punto del evento distinto del catálogo");
        long event = events.create(form).getId(); events.transition(event, EventStatus.OPEN);
        long a = person(event, "Samuel", interest), b = person(event, "Julián", interest);
        long match = matching.search(a); service.accept(match, a);
        assertThat(service.get(match, a).status()).isEqualTo(MatchStatus.OFFERED);
        assertThat(service.get(match, a).meetingPoint()).isNull(); assertThat(service.get(match, b).word()).isNull();
        service.accept(match, b); var first = service.get(match, a); var second = service.get(match, b);
        assertThat(first.meetingPoint()).isEqualTo(point); assertThat(first.word()).isEqualTo(prefix + "-" + Long.toString(match, 36).toUpperCase(java.util.Locale.ROOT));
        assertThat(first.activity()).isEqualTo(activity);
        assertThat(second.meetingPoint()).isEqualTo(first.meetingPoint()); assertThat(second.word()).isEqualTo(first.word());
        assertThat(second.activity()).isEqualTo(first.activity());
        EventForm edit = EventForm.from(events.get(event)); edit.setMeetingPoint("Otro punto"); events.update(event, edit);
        service.accept(match, a); assertThat(service.get(match, a).word()).isEqualTo(first.word());
        assertThat(service.get(match, b).meetingPoint()).isEqualTo(point);
        service.confirm(match, a); assertThat(participants.get(a).points()).isZero();
        service.confirm(match, b); service.confirm(match, b); service.confirm(match, a);
        assertThat(service.get(match, a).status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(participants.get(a).points()).isEqualTo(10); assertThat(participants.get(b).points()).isEqualTo(10);
        long next = matching.search(a); service.accept(next, a); service.accept(next, b);
        assertThat(service.get(next, a).word()).startsWith(prefix + "-").isNotEqualTo(first.word());
        assertThat(matches.count()).isEqualTo(2);
    }
    @Test void multipleSharedInterestsUseTheExistingDeterministicTopicOrder() {
        var a = ActivityCatalog.encounter("Tecnología · Anime · Videojuegos", 123, "SOFA 2026", "Entrada");
        var b = ActivityCatalog.encounter("Videojuegos · Anime · Tecnología", 123, "SOFA 2026", "Entrada");
        assertThat(a).isEqualTo(b); assertThat(a.meetingPoint()).isEqualTo("Zona Gamer · Pabellón 18");
        assertThat(a.word()).isEqualTo("PIXEL-3F");
    }
    @Test void otherEventsKeepTheirConfiguredPointAndOriginalCatalog() {
        var technology = ActivityCatalog.encounter("Tecnología", 35, "Otro evento", "Entrada principal");
        assertThat(technology.meetingPoint()).isEqualTo("Entrada principal"); assertThat(technology.word()).isEqualTo("NODO-Z");
        assertThat(technology.activity()).isEqualTo("Hablen de una tecnología que les parezca interesante.");
        var neutral = ActivityCatalog.encounter("", 36, "Otro evento", "Cafetería");
        assertThat(neutral.meetingPoint()).isEqualTo("Cafetería"); assertThat(neutral.word()).isEqualTo("PING-10");
        assertThat(neutral.activity()).doesNotContain("SOFA");
    }
}
