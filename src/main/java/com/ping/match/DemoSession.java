package com.ping.match;
import jakarta.servlet.http.HttpSession;
import com.ping.web.BusinessRuleException;
public final class DemoSession {
    private DemoSession() { }
    public static long participantId(HttpSession session) {
        if (session.getAttribute("pingDemoParticipantId") instanceof Long id) return id;
        throw new com.ping.web.SessionRequiredException();
    }
}
