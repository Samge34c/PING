package com.ping.match;
public record MatchView(long id, MatchStatus status, String otherAlias, String commonInterests, String otherInterests,
                        boolean accepted, boolean otherAccepted, boolean confirmed, boolean otherConfirmed,
                        String meetingPoint, String word, String activity, String feedback, boolean completionPending, java.time.Instant createdAt, boolean arrivalRequired, boolean arrived, boolean otherArrived, String meetingDirections, String activityTitle, int rewardPoints, String floorplanId, Double floorplanX, Double floorplanY,int verificationPolicy,boolean verified,boolean otherVerified,com.ping.participant.Avatar otherAvatar,com.ping.encounter.CheckMethod activityCheck,String checkInstructions,String responsePrompt,boolean cancelledByOrganization) {
    public boolean staffReview(){return activityCheck==com.ping.encounter.CheckMethod.STAFF_REVIEW;}
    public boolean atDelivery(){return activityCheck==com.ping.encounter.CheckMethod.AT_DELIVERY;}
    public java.util.List<String> commonTopics(){return commonInterests==null||commonInterests.isBlank()?java.util.List.of():java.util.List.of(commonInterests.split(" · "));}
    public java.util.List<String> otherTopics(){return otherInterests==null||otherInterests.isBlank()?java.util.List.of():java.util.List.of(otherInterests.split(" · "));}
    /** DEC-070: long directions show their first sentence; the rest opens with «Ver indicaciones». */
    public String directionsLead(){return EncounterProgress.split(meetingDirections)[0];}
    public String directionsMore(){return EncounterProgress.split(meetingDirections)[1];}
    public String commonPreview(){return String.join(" · ",commonTopics().stream().limit(3).toList());}
    public boolean canConfirm() { return verificationPolicy<2 || arrived&&otherArrived&&verified&&otherVerified; }
}
