package com.ping.experience;

import java.util.Map;
import com.ping.demo.DemoScenarioRepository;
import com.ping.match.MatchStatus;
import com.ping.match.MatchView;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!bootstrap") @Transactional(readOnly = true)
public class DemoRecognitionHints {
    private static final Map<String, String> HINTS = Map.of(
        "Samuel", "Camiseta negra y morral", "Julián", "Gorra roja",
        "Ana", "Tote bag y camiseta de anime", "Carlos", "Chaqueta azul");
    private final DemoScenarioRepository scenarios;
    private final com.ping.match.MatchRepository matches;
    private final com.ping.participant.ParticipantRepository participants;
    public DemoRecognitionHints(DemoScenarioRepository scenarios, com.ping.match.MatchRepository matches, com.ping.participant.ParticipantRepository participants) {
        this.scenarios = scenarios; this.matches = matches; this.participants = participants;
    }
    public String forMatch(long eventId, long self, MatchView match) {
        if (match == null || match.status() != MatchStatus.ACTIVE) return null;
        var actual = matches.findById(match.id()).orElse(null);
        if (actual == null || actual.getStatus() != MatchStatus.ACTIVE || !actual.owns(self)) return null;
        long otherId = actual.getFirst().getId() == self ? actual.getSecond().getId() : actual.getFirst().getId();
        // Only the four original demo people, not an attendee with a coinciding alias.
        if (participants.findByEventIdOrderByIdAsc(eventId).stream().limit(4).noneMatch(p -> p.getId() == otherId)) return null;
        return scenarios.findById(1L).filter(s -> s.getEvent() != null && s.getEvent().getId() == eventId
            && EventExperienceCatalog.forEvent(s.getEvent().getName()) != null)
            .map(s -> HINTS.get(match.otherAlias())).orElse(null);
    }
}
