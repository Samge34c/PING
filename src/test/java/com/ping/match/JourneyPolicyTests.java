package com.ping.match;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/** DEC-073: encounter policy switch (2 by default, 3 = one scan) and the single verification rule. */
class JourneyPolicyTests {
    @Test void defaultsToPolicyTwoAndAcceptsOnlyTwoOrThree() {
        var policy = new JourneyPolicy();
        assertThat(policy.encounterVersion()).isEqualTo(2);
        assertThat(policy.version()).as("receipts and guides are not affected").isEqualTo(2);
        policy.configure(3); assertThat(policy.encounterVersion()).isEqualTo(3); assertThat(policy.version()).isEqualTo(2);
        policy.configure(2); assertThat(policy.encounterVersion()).isEqualTo(2);
        for (int wrong : new int[]{0, 1, 4}) assertThatThrownBy(() -> policy.configure(wrong)).isInstanceOf(IllegalStateException.class);
    }
    @Test void historicalFixturesKeepTheirOldVersionForEncounters() {
        var legacy = new JourneyPolicy() { @Override public int version() { return 1; } };
        legacy.configure(3);
        assertThat(legacy.encounterVersion()).isEqualTo(1);
    }
    @Test void verificationRuleNeedsBothScansExceptOnPolicyThree() {
        for (int policy : new int[]{1, 2}) {
            assertThat(Match.verificationComplete(policy, true, true)).isTrue();
            assertThat(Match.verificationComplete(policy, true, false)).isFalse();
            assertThat(Match.verificationComplete(policy, false, true)).isFalse();
        }
        assertThat(Match.verificationComplete(3, true, false)).isTrue();
        assertThat(Match.verificationComplete(3, false, true)).isTrue();
        assertThat(Match.verificationComplete(3, false, false)).isFalse();
        assertThat(EncounterProgress.phase(3, false, true, true, false, true)).isEqualTo("activity");
        assertThat(EncounterProgress.phase(2, false, true, true, false, true)).isEqualTo("verify");
        assertThat(EncounterProgress.phase(3, false, true, false, true, false)).as("arrivals are still required").isEqualTo("arrive");
    }
}
