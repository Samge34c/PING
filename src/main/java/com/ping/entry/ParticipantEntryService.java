package com.ping.entry;

import com.ping.participant.*;
import com.ping.interest.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.List;

@Service @Profile("!bootstrap")
public class ParticipantEntryService {
    private final com.ping.auth.AccountService accounts;
    private final ParticipantService participants;
    private final InterestService interests;
    public ParticipantEntryService(ParticipantService participants, InterestService interests,com.ping.auth.AccountService accounts) { this.accounts=accounts;
        this.participants = participants; this.interests = interests;
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long createForAccount(long account,String alias,Long eventId,List<Long> ids,Avatar avatar){accounts.lock(account);var old=accounts.participant(account,eventId);if(old.isPresent())return old.get();long id=create(alias,eventId,ids,avatar);accounts.bind(account,id,eventId);return id;}
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
