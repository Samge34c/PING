package com.ping.demo;
import java.util.*;
import com.ping.event.*;
import com.ping.participant.*;
import com.ping.interest.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @Profile("!bootstrap") @Transactional(readOnly = true)
public class DemoPreparationService {
    public record Person(long id, String alias, List<String> interests) { }
    public record Scenario(long eventId, String name, EventStatus status, List<Person> people) { }
    private final DemoScenarioRepository scenarios;
    private final EventService events;
    private final ParticipantService participants;
    private final ParticipantRepository people;
    private final InterestService interests;
    public DemoPreparationService(DemoScenarioRepository scenarios, EventService events, ParticipantService participants, ParticipantRepository people, InterestService interests) {
        this.scenarios = scenarios; this.events = events; this.participants = participants; this.people = people; this.interests = interests;
    }
    public Scenario current() { return scenarios.findById(1L).map(DemoScenario::getEvent).map(this::view).orElse(null); }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Scenario prepare() {
        DemoScenario marker = scenarios.lockScenario().orElseThrow();
        if (marker.getEvent() != null) return view(marker.getEvent());
        EventForm f = new EventForm(); f.setName("Feria Universitaria 2026"); f.setDescription("Escenario DEMO académico, preparado manualmente. Datos de demostración."); f.setMeetingPoint("Entrada principal");
        Event event = events.create(f); events.transition(event.getId(), EventStatus.OPEN);
        add(event, "Samuel", "Videojuegos", "Tecnología"); add(event, "Julián", "Videojuegos", "Música");
        add(event, "Ana", "Cine", "Fotografía"); add(event, "Carlos", "Deportes", "Viajes");
        marker.setEvent(event); return view(event);
    }
    private void add(Event event, String alias, String... names) {
        ParticipantForm f = new ParticipantForm(); f.setAlias(alias); f.setEventId(event.getId());
        var p = participants.create(f); InterestSelectionForm selected = new InterestSelectionForm(); selected.setVersion(p.version());
        selected.setInterestIds(interests.catalog().stream().filter(i -> Arrays.asList(names).contains(i.getName())).map(Interest::getId).toList()); interests.save(p.id(), selected);
    }
    private Scenario view(Event event) {
        return new Scenario(event.getId(), event.getName(), event.getStatus(), people.findByEventIdOrderByIdAsc(event.getId()).stream()
            .map(p -> new Person(p.getId(), p.getAlias(), p.getInterests().stream().map(Interest::getName).sorted().toList())).toList());
    }
}
