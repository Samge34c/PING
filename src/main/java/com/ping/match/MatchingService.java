package com.ping.match;

import java.util.*;
import com.ping.event.*;
import com.ping.participant.*;
import com.ping.interest.Interest;
import com.ping.web.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!bootstrap")
public class MatchingService {
    private final ParticipantRepository participants;
    private final EventService events;
    private final MatchRepository matches;
    private final MatchReservationRepository reservations;
    public MatchingService(ParticipantRepository participants, EventService events, MatchRepository matches, MatchReservationRepository reservations) {
        this.participants = participants; this.events = events; this.matches = matches; this.reservations = reservations;
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public long search(long participantId) {
        Event event = events.lock(participants.eventId(participantId).orElseThrow(() -> new NotFoundException("No encontramos ese participante.")));
        Participant self = participants.findById(participantId).orElseThrow();
        // A repeated search returns its existing offer rather than creating another one.
        var held = matches.forParticipant(participantId).stream().filter(m -> m.getStatus() == MatchStatus.OFFERED || m.getStatus() == MatchStatus.ACTIVE).findFirst();
        if (held.isPresent()) return held.get().getId();
        if (!event.canStartNewMatches()) throw new BusinessRuleException("El evento debe estar abierto para buscar una conexión.");
        if (!self.isAvailable()) throw new BusinessRuleException("Marca tu disponibilidad antes de buscar.");
        Set<Long> occupied = new HashSet<>(reservations.findAll().stream().map(MatchReservation::getParticipantId).toList());
        List<Match> rejected = matches.findByEventIdAndStatus(event.getId(), MatchStatus.CANCELLED);
        Participant candidate = participants.findByEventIdAndAvailableTrue(event.getId()).stream()
            .filter(p -> !p.getId().equals(self.getId()) && !occupied.contains(p.getId()))
            .filter(p -> rejected.stream().noneMatch(m -> m.excludedPair(self, p)))
            .sorted(Comparator.<Participant>comparingInt(p -> common(self, p).size()).reversed()
                .thenComparing(Participant::getWaitingSince).thenComparing(Participant::getId))
            .findFirst().orElseThrow(() -> new BusinessRuleException("No hay personas disponibles para una nueva propuesta. Puedes volver a buscar más tarde."));
        Match match = matches.saveAndFlush(new Match(event, self, candidate, String.join(" · ", common(self, candidate)), names(self), names(candidate)));
        reservations.saveAllAndFlush(List.of(new MatchReservation(self.getId(), match), new MatchReservation(candidate.getId(), match)));
        return match.getId();
    }
    private List<String> common(Participant a, Participant b) {
        Set<Long> ids = new HashSet<>(b.getInterests().stream().map(Interest::getId).toList());
        return a.getInterests().stream().filter(i -> ids.contains(i.getId())).map(Interest::getName).sorted().toList();
    }
    private String names(Participant p) { return String.join(" · ", p.getInterests().stream().map(Interest::getName).sorted().toList()); }
}
