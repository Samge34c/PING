package com.ping.match;
public record MatchView(long id, MatchStatus status, String otherAlias, String commonInterests, String otherInterests,
                        boolean accepted, boolean otherAccepted, boolean confirmed, boolean otherConfirmed,
                        String meetingPoint, String word, String activity, String feedback, boolean completionPending, java.time.Instant createdAt, boolean arrivalRequired, boolean arrived, boolean otherArrived, String meetingDirections, String activityTitle, int rewardPoints, String floorplanId, Double floorplanX, Double floorplanY,int verificationPolicy,boolean verified,boolean otherVerified) {
    public boolean canConfirm() { return verificationPolicy<2 || arrived&&otherArrived&&verified&&otherVerified; }
}
