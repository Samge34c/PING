package com.ping.entry;

import com.ping.participant.*;
import com.ping.interest.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.List;

@Service @Profile("!bootstrap")
public class ParticipantEntryService {
    private final ParticipantService participants;
    private final InterestService interests;
    public ParticipantEntryService(ParticipantService participants, InterestService interests) {
        this.participants = participants; this.interests = interests;
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long create(String alias,Long eventId,List<Long> ids,Avatar avatar){long id=create(alias,eventId,ids);participants.avatar(id,avatar);return id;}
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long create(String alias, Long eventId, List<Long> ids) {
        ParticipantForm form = new ParticipantForm();
        form.setAlias(alias); form.setEventId(eventId); form.setAvailable(false);
        long id = participants.create(form).id();
        InterestSelectionForm selection = interests.selection(id);
        selection.setInterestIds(ids); interests.save(id, selection);
        return id;
    }
}
