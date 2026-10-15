package com.ping.experience;

import com.ping.match.MatchView;
import com.ping.participant.RecognitionHintService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service @Profile("!bootstrap")
public class DemoRecognitionHints {
    private final RecognitionHintService hints;
    public DemoRecognitionHints(RecognitionHintService hints) { this.hints = hints; }
    public String forMatch(long eventId, long self, MatchView view) {
        if (view == null) return null;
        var authorized = hints.other(view.id(), self);
        return authorized == null ? null : authorized.text();
    }
}
