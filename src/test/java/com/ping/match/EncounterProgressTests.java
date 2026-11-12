package com.ping.match;

import java.time.Instant;
import com.ping.encounter.CheckMethod;
import com.ping.review.ActivityReviewService.Review;
import com.ping.review.ActivityReviewService.Status;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** DEC-070: the single card of an active encounter follows the real policy and state; no step is invented or skipped. */
class EncounterProgressTests {
    static final String REST = "Expliquen qué les gusta de ella y escuchen juntos un fragmento corto, sin auriculares, en un lugar donde puedan oírse bien; luego cada uno dice qué recuerdo le trae y si la recomendaría a alguien del evento.";
    record S(int policy, CheckMethod check, boolean arrived, boolean otherArrived, boolean verified, boolean otherVerified,
             boolean confirmed, boolean otherConfirmed, boolean pending) { }

    static MatchView view(S s) {
        return new MatchView(1, MatchStatus.ACTIVE, "Nico", "", "", true, true, s.confirmed(), s.otherConfirmed(), "Mesa", "faro",
                "Compartan una canción. " + REST, null, s.pending(), Instant.now(), s.policy() >= 2,
                s.arrived(), s.otherArrived(), null, "Una recomendación", 20, null, null, null, s.policy(), s.verified(), s.otherVerified(),
                null, s.check(), "Una foto", null, false);
    }
    static EncounterProgress of(S s, Review review) { return EncounterProgress.of(view(s), review); }
    static Review review(Status status) { return new Review(1, status, "nota", "Falta el nombre", Instant.now(), null, 1, 2); }

    @Test void policyTwoAdvancesOnlyWithEveryArrivalAndVerification() {
        var b = CheckMethod.BILATERAL;
        assertThat(of(new S(2, b, false, true, false, false, false, false, false), null)).extracting(EncounterProgress::phase, EncounterProgress::sub).containsExactly("arrive", "go");
        assertThat(of(new S(2, b, true, false, false, false, false, false, false), null)).extracting(EncounterProgress::phase, EncounterProgress::sub).containsExactly("arrive", "wait");
        assertThat(of(new S(2, b, true, true, false, true, false, false, false), null)).extracting(EncounterProgress::phase, EncounterProgress::sub).containsExactly("verify", "scan");
        assertThat(of(new S(2, b, true, true, true, false, false, false, false), null)).extracting(EncounterProgress::phase, EncounterProgress::sub).containsExactly("verify", "show");
        var activity = of(new S(2, b, true, true, true, true, false, false, false), null);
        assertThat(activity).extracting(EncounterProgress::phase, EncounterProgress::sub, EncounterProgress::current).containsExactly("activity", "confirm", 3);
        assertThat(activity.lead()).isEqualTo("Compartan una canción.");
        assertThat(activity.moreInstructions()).isEqualTo(REST);
        assertThat(EncounterProgress.split("Compartan una canción. Expliquen por qué.")).containsExactly("Compartan una canción. Expliquen por qué.", null);
        // Content of the activity is produced only in the activity phase.
        assertThat(of(new S(2, b, true, true, true, false, false, false, false), null).lead()).isNull();
    }

    @Test void legacyPolicyHasNoArrivalOrCodeStep() {
        var legacy = of(new S(1, CheckMethod.BILATERAL, false, false, false, false, false, false, false), null);
        assertThat(legacy.phase()).isEqualTo("activity");
        assertThat(legacy.steps()).extracting(EncounterProgress.Step::label).containsExactly("Actividad", "Confirmar");
    }

    @Test void staffReviewKeepsItsOrderAndAllowsConfirmingWhileReviewed() {
        var c = CheckMethod.STAFF_REVIEW;
        var ready = new S(2, c, true, true, true, true, false, false, false);
        assertThat(of(ready, null).sub()).isEqualTo("submit");
        assertThat(of(ready, review(Status.CHANGES)).sub()).isEqualTo("fix");
        assertThat(of(ready, review(Status.PENDING))).extracting(EncounterProgress::sub, EncounterProgress::currentLabel).containsExactly("confirm", "Revisión");
        assertThat(of(new S(2, c, true, true, true, true, true, true, false), review(Status.PENDING)).sub()).isEqualTo("review");
        assertThat(of(new S(2, c, true, true, true, true, true, false, false), review(Status.APPROVED)).sub()).isEqualTo("wait");
    }

    @Test void deliveryCheckIsMarkedAsDoneAndFinishingNeverClaimsCompletion() {
        var d = CheckMethod.AT_DELIVERY;
        assertThat(of(new S(2, d, true, true, true, true, false, true, false), null)).extracting(EncounterProgress::sub, EncounterProgress::detail)
                .containsExactly("mark", "Nico ya la marcó como hecha. Falta tu parte.");
        assertThat(of(new S(2, d, true, true, true, true, true, false, false), null).sub()).isEqualTo("wait");
        var finishing = of(new S(2, d, true, true, true, true, true, true, true), null);
        assertThat(finishing).extracting(EncounterProgress::phase, EncounterProgress::sub).containsExactly("finishing", "retry");
        assertThat(finishing.lead()).isNull();
    }
}
