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
        if (marker.getEvent() != null) {
            Event event = marker.getEvent();
            if ("Feria Universitaria 2026".equals(event.getName())) {
                EventForm f = EventForm.from(events.lock(event.getId())); configure(f); events.update(event.getId(), f);
                configurePeople(event);
            }
            return view(event);
        }
        EventForm f = new EventForm(); configure(f);
        Event event = events.create(f); events.transition(event.getId(), EventStatus.OPEN);
        configurePeople(event);
        marker.setEvent(event); return view(event);
    }
    private void configure(EventForm form) {
        form.setName("SOFA 2026");
        form.setDescription("Escenario demo académico inspirado en SOFA 2026, realizado en Corferias Bogotá del 8 al 12 de octubre de 2026.");
        form.setMeetingPoint("Meet & Greet · Pabellón 23");
    }
    private void configurePeople(Event event) {
        add(event, "Samuel", "Videojuegos", "Anime"); add(event, "Julián", "Videojuegos", "Tecnología");
        add(event, "Ana", "Anime", "Fotografía"); add(event, "Carlos", "Cine", "Música");
    }
    private void add(Event event, String alias, String... names) {
        var existing = people.findByEventIdOrderByIdAsc(event.getId()).stream().filter(p -> p.getAlias().equals(alias)).findFirst();
        long id;
        if (existing.isPresent()) id = existing.get().getId();
        else {
            ParticipantForm f = new ParticipantForm(); f.setAlias(alias); f.setEventId(event.getId());
            id = participants.create(f).id();
        }
        InterestSelectionForm selected = interests.selection(id);
        selected.setInterestIds(interests.catalog().stream().filter(i -> Arrays.asList(names).contains(i.getName())).map(Interest::getId).toList()); interests.save(id, selected);
    }
    private Scenario view(Event event) {
        return new Scenario(event.getId(), event.getName(), event.getStatus(), people.findByEventIdOrderByIdAsc(event.getId()).stream()
            .map(p -> new Person(p.getId(), p.getAlias(), p.getInterests().stream().map(Interest::getName).sorted().toList())).toList());
    }
}
