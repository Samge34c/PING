package com.ping.match;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
/**
 * New runtime activations/receipts use policy 2; historical rows keep their frozen version.
 * DEC-073: {@link #encounterVersion()} is the policy frozen on NEW encounters. It comes from
 * {@code ping.journey.encounter-policy} (2 by default; 3 = one valid scan verifies the encounter for both).
 * Receipts and guides keep using {@link #version()}.
 */
@Component @Profile("!bootstrap") public class JourneyPolicy {
    private int encounter = 2;
    @Autowired(required = false)
    void configure(@Value("${ping.journey.encounter-policy:2}") int value) {
        if (value != 2 && value != 3) throw new IllegalStateException("ping.journey.encounter-policy must be 2 or 3");
        encounter = value;
    }
    public int version(){return 2;}
    /** Historical fixtures that override {@link #version()} below 2 keep that version for encounters too. */
    public int encounterVersion(){return version() < 2 ? version() : encounter;}
}
