package com.ping.participant;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @Profile("!bootstrap") @Transactional(propagation = Propagation.MANDATORY)
public class CompletionRewardService {
    private final ParticipantRepository participants;
    public CompletionRewardService(ParticipantRepository participants) { this.participants = participants; }
    public void grant(long participantId) { participants.findById(participantId).orElseThrow().rewardCompletedMatch(); }
    public void reverse(long participantId) { participants.findById(participantId).orElseThrow().reverseCompletionReward(); }
}
