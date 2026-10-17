package com.ping.match;
public record MatchView(long id, MatchStatus status, String otherAlias, String commonInterests, String otherInterests,
                        boolean accepted, boolean otherAccepted, boolean confirmed, boolean otherConfirmed,
                        String meetingPoint, String word, String activity, String feedback, boolean completionPending, java.time.Instant createdAt, boolean arrivalRequired, boolean arrived, boolean otherArrived) {
    public boolean canConfirm() { return !arrivalRequired || (arrived && otherArrived); }
}
