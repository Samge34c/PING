package com.ping.match;

import java.util.List;
import com.ping.participant.ParticipantView;

/**
 * DEC-066. What the participant has really done in this event, from their own encounter records only.
 * Balance is the stored balance; catalog values are never summed as earned points.
 */
public record ActivityProgress(long balance, long earned, List<Done> completed, MatchView active) {
    public record Done(long matchId, String title, int points, java.time.Instant createdAt) { }
    public int pending() { return active == null ? 0 : active.rewardPoints(); }
    public static ActivityProgress of(ParticipantView participant, List<MatchView> history) {
        var done = history.stream().filter(m -> m.status() == MatchStatus.COMPLETED)
            .map(m -> new Done(m.id(), m.activityTitle() == null || m.activityTitle().isBlank() ? (m.activity() == null ? "Actividad del encuentro" : m.activity()) : m.activityTitle(), m.rewardPoints(), m.createdAt()))
            .toList();
        var active = history.stream().filter(m -> m.status() == MatchStatus.ACTIVE).findFirst().orElse(null);
        return new ActivityProgress(participant.points(), done.stream().mapToLong(Done::points).sum(), done, active);
    }
}
