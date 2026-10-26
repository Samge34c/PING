package com.ping.match;

import java.util.Map;
import java.util.UUID;
import com.ping.event.*;
import com.ping.participant.*;
import com.ping.support.TestBrowser;
import com.ping.web.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:live-state;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class MatchLiveStateTests {
    @Autowired MatchLiveState live;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean MatchService matches;
    @Autowired MatchingService matching;
    @Autowired ParticipantService participants;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean EventService events;
    @Autowired JdbcTemplate jdbc;
    @LocalServerPort int port;

    long[] pair() {
        var form = new EventForm(); form.setName("Estado ficticio " + UUID.randomUUID());
        long event = events.create(form).getId(); events.transition(event, EventStatus.OPEN);
        long[] pair = new long[2];
        for (int i = 0; i < 2; i++) {
            var p = new ParticipantForm(); p.setAlias("Persona ficticia " + i); p.setAvailable(true); p.setEventId(event);
            pair[i] = participants.create(p).id();
        }
        return pair;
    }

    void agreesWithHtml(long self, String route) throws Exception {
        try (var browser = new TestBrowser(port)) {
            browser.get("/demo"); browser.post("/demo", Map.of("participantId", Long.toString(self)));
            String html = browser.get(route).body();
            assertThat(html).contains("data-ping-live=\"" + live.forParticipant(self).get("signature") + "\"");
        }
    }

    @Test void signatureKeepsBothOrientationsAndEveryVisibleTransition() throws Exception {
        long[] p = pair(); agreesWithHtml(p[0], "/me");
        long id = matching.search(p[0]);
        for (long self : p) agreesWithHtml(self, "/matches/" + id);
        matches.accept(id, p[0]);
        for (long self : p) agreesWithHtml(self, "/matches/" + id);
        matches.accept(id, p[1]); matches.arrive(id, p[1]);
        for (long self : p) agreesWithHtml(self, "/matches/" + id);
        jdbc.update("update ping_matches set first_verified=true, second_confirmed=true, completion_pending=true where id=?", id);
        jdbc.update("update participants set recognition_hint='PISTA PRIVADA',version=version+1 where id=?", p[1]);
        for (long self : p) {
            agreesWithHtml(self, "/matches/" + id);
            assertThat(live.forParticipant(self)).containsOnlyKeys("signature", "matchStatus", "recognitionRevision");
            assertThat(live.forParticipant(self).toString()).doesNotContain("PISTA PRIVADA", "activity", "word", "floorplan");
        }
        events.transition(participants.get(p[0]).eventId(), EventStatus.PAUSED);
        agreesWithHtml(p[0], "/matches/" + id);
    }

    @Test void terminalHistoryAndOtherEventsDoNotChangeTheCurrentState() throws Exception {
        long[] p = pair(), unrelated = pair(); long id = matching.search(p[0]);
        Map<String, String> original = live.forParticipant(p[0]);
        for (int i = 0; i < 24; i++) jdbc.update("""
                insert into ping_matches(event_id,first_id,second_id,status,first_wait,second_wait,
                    common_interests,first_interests,second_interests,created_at)
                select event_id,first_id,second_id,'CANCELLED',first_wait,second_wait,
                    common_interests,first_interests,second_interests,created_at from ping_matches where id=?
                """, id);
        assertThat(live.forParticipant(p[0])).isEqualTo(original);
        assertThat(live.forParticipant(unrelated[0])).containsEntry("matchStatus", "NONE");
        assertThat(live.forParticipant(unrelated[0]).get("signature")).doesNotContain(":" + id + ":OFFERED");
        matches.cancel(id, p[0]);
        assertThat(live.forParticipant(p[0])).containsEntry("matchStatus", "NONE");
        agreesWithHtml(p[0], "/me");
        assertThatThrownBy(() -> live.forParticipant(Long.MAX_VALUE)).isInstanceOf(NotFoundException.class);
    }

    @Test void openingAnOfferedOrActiveMissionDoesNotLoadHistoryAndReadsTheEventOnce() throws Exception {
        long[] p = pair(); long id = matching.search(p[0]);
        try (var browser = new TestBrowser(port)) {
            browser.get("/demo"); browser.post("/demo", Map.of("participantId", Long.toString(p[0])));
            var checks = new org.assertj.core.api.SoftAssertions();
            for (var status : java.util.List.of(MatchStatus.OFFERED, MatchStatus.ACTIVE)) {
                if (status == MatchStatus.ACTIVE) { matches.accept(id, p[0]); matches.accept(id, p[1]); }
                String expected = live.forParticipant(p[0]).get("signature");
                org.mockito.Mockito.clearInvocations(matches, events);
                var page = browser.get("/matches/" + id);
                assertThat(page.statusCode()).isEqualTo(200);
                assertThat(page.body()).contains("data-ping-live=\"" + expected + "\"");
                long historyReads = org.mockito.Mockito.mockingDetails(matches).getInvocations().stream()
                        .filter(i -> i.getMethod().getName().equals("list")).count();
                long eventReads = org.mockito.Mockito.mockingDetails(events).getInvocations().stream()
                        .filter(i -> i.getMethod().getName().equals("get")).count();
                System.out.println("PERF mission " + status + " history_reads=" + historyReads + " event_reads=" + eventReads);
                checks.assertThat(historyReads).as("history reads for %s", status).isZero();
                checks.assertThat(eventReads).as("event reads for %s", status).isEqualTo(1);
            }
            checks.assertAll();
        }
    }

    @Test void ownCredentialRevisionIsScopedAndOldPolicyIsCompatible() throws Exception {
        long[] p = pair(); long id = matching.search(p[0]);
        matches.accept(id, p[0]); matches.accept(id, p[1]);
        jdbc.update("update ping_matches set verification_policy=2 where id=?", id);
        String before = live.forParticipant(p[0]).get("signature");
        String otherBefore = live.forParticipant(p[1]).get("signature");
        jdbc.update("""
                insert into purpose_credentials(id,issue_key,purpose,context_id,event_id,issuer_id,token_hash,manual_hash,expires_at)
                values(?,?,'ENCOUNTER',?,?,?,?,?,CURRENT_TIMESTAMP)
                """, UUID.randomUUID().toString(), UUID.randomUUID().toString(), id,
                participants.get(p[0]).eventId(), p[0], "a".repeat(64), "b".repeat(64));
        assertThat(live.forParticipant(p[0]).get("signature")).isNotEqualTo(before).endsWith(":1");
        assertThat(live.forParticipant(p[1]).get("signature")).isEqualTo(otherBefore);
        for (long self : p) agreesWithHtml(self, "/matches/" + id);
        jdbc.update("update ping_matches set verification_policy=1 where id=?", id);
        assertThat(live.forParticipant(p[0]).get("signature")).isEqualTo(before);
        agreesWithHtml(p[0], "/matches/" + id);
    }
}
